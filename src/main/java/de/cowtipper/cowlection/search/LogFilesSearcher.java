package de.cowtipper.cowlection.search;

import de.cowtipper.cowlection.config.MooConfig;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

class LogFilesSearcher {
    private static final Pattern LOG_FILE_PATTERN = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})-\\d+\\.log\\.gz$");
    /**
     * Log4j.xml PatternLayout: [%d{HH:mm:ss}] [%t/%level]: %msg%n
     * Log line: [TIME] [THREAD/LEVEL]: [CHAT] msg
     * examples:
     * - [13:33:37] [Client thread/INFO]: [CHAT] Hello World
     * - [08:15:42] [Client thread/ERROR]: Item entity 9001 has no item?!
     */
    private static final Pattern LOG4J_PATTERN = Pattern.compile("^\\[(?<timeHours>[\\d]{2}):(?<timeMinutes>[\\d]{2}):(?<timeSeconds>[\\d]{2})] \\[(?<thread>[^/]+)/(?<logLevel>[A-Z]+)]:(?<isChat> \\[CHAT])? (?<message>.*)$");
    private int analyzedFilesWithHits = 0;

    LogSearchResults searchFor(String searchQuery, boolean chatOnly, boolean matchCase, boolean removeFormatting, LocalDate dateStart, LocalDate dateEnd) throws IOException {
        LogSearchResults logSearchResults = new LogSearchResults();
        long fileSizeLimit = MooConfig.getMaxLogFileSize();
        long latestLogSizeLimit = MooConfig.getMaxLatestLogFileSize();
        for (String logsDirPath : MooConfig.logsDirs) {
            File logsDir = new File(logsDirPath);
            if (!logsDir.exists() || !logsDir.isDirectory()) {
                continue;
            }
            try (Stream<Path> paths = Files.find(logsDir.toPath(), 1, (path, attr) -> {
                if (!attr.isRegularFile()) {
                    return false;
                }
                String fileName = path.getFileName().toString();
                return fileName.endsWith(".log.gz") || "latest.log".equals(fileName);
            }).collect(Collectors.toList()).parallelStream()) {
                paths.forEach(path -> {
                    String fileName = path.getFileName().toString();
                    if (fileName.endsWith("z")) { // .log.gz
                        Matcher fileNameMatcher = LOG_FILE_PATTERN.matcher(fileName);
                        if (fileNameMatcher.matches()) {
                            LocalDate fileLocalDate = LocalDate.of(Integer.parseInt(fileNameMatcher.group(1)),
                                    Integer.parseInt(fileNameMatcher.group(2)), Integer.parseInt(fileNameMatcher.group(3)));
                            if (!fileLocalDate.isBefore(dateStart) && !fileLocalDate.isAfter(dateEnd)) {
                                if (path.toFile().length() > fileSizeLimit) {
                                    // .log.gz file too large
                                    logSearchResults.addSkippedFile();
                                } else {
                                    logSearchResults.addAnalyzedFile();
                                    logSearchResults.addSearchResults(analyzeFile(path, true, fileLocalDate, searchQuery, chatOnly, matchCase, removeFormatting));
                                }
                            }
                        }
                    } else if (fileName.equals("latest.log")) {
                        LocalDate lastModified = Instant.ofEpochMilli(path.toFile().lastModified()).atZone(ZoneId.systemDefault()).toLocalDate();
                        if (!lastModified.isBefore(dateStart) && !lastModified.isAfter(dateEnd)) {
                            if (path.toFile().length() > latestLogSizeLimit) {
                                // latest.log too large
                                logSearchResults.addSkippedFile();
                            } else {
                                logSearchResults.addAnalyzedFile();
                                logSearchResults.addSearchResults(analyzeFile(path, false, lastModified, searchQuery, chatOnly, matchCase, removeFormatting));
                            }
                        }
                    }
                });
            } catch (IOException e) {
                throw new IOException(EnumChatFormatting.DARK_RED + "ERROR: An error occurred trying to read/parse '" + EnumChatFormatting.RED + logsDirPath + EnumChatFormatting.DARK_RED + "':\n"
                        + EnumChatFormatting.GOLD + e.getLocalizedMessage(), e);
            }
        }

        if (logSearchResults.getAnalyzedFiles() == 0) {
            // no files were analyzed
            int skippedFileCounter = logSearchResults.getSkippedFiles();
            if (skippedFileCounter > 0) {
                throw new FileNotFoundException(EnumChatFormatting.DARK_RED + "ERROR: No Minecraft log files could be found for the selected date range.\n"
                        + EnumChatFormatting.RED + skippedFileCounter + EnumChatFormatting.DARK_RED + " log files were skipped because they are too large (" + EnumChatFormatting.RED + ".log.gz" + EnumChatFormatting.DARK_RED + " files >" + FileUtils.byteCountToDisplaySize(MooConfig.getMaxLogFileSize()) + "; " + EnumChatFormatting.RED + "latest.log" + EnumChatFormatting.DARK_RED + " >" + FileUtils.byteCountToDisplaySize(MooConfig.getMaxLatestLogFileSize()) + ").\n"
                        + EnumChatFormatting.RED + "Please check if the dates as well as the directories of the log files are set correctly (Log Search ➡ Settings [top right corner]).\n"
                        + EnumChatFormatting.DARK_RED + "You could also increase the maximum allowed log file size to be searched (Log Search ➡ Settings), but note that each file must be unzipped before it can be analyzed, which can make the log file search take significantly longer for large files.");
            } else {
                throw new FileNotFoundException(EnumChatFormatting.DARK_RED + "ERROR: No Minecraft log files could be found for the selected date range.\n"
                        + EnumChatFormatting.RED + "Please check if the dates as well as the directories of the log files are set correctly (Log Search ➡ Settings [top right corner]).");
            }
        } else {
            logSearchResults.setAnalyzedFilesWithHits(analyzedFilesWithHits);
            return logSearchResults;
        }
    }

    private List<LogEntry> analyzeFile(Path path, boolean isGzipped, LocalDate date, String searchTerm, boolean chatOnly, boolean matchCase, boolean removeFormatting) {
        List<LogEntry> searchResults = new ArrayList<>();
        boolean foundSearchTermInFile = false;
        try (BufferedReader in = (isGzipped
                ? new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(path.toFile())))) // ....log.gz
                : new BufferedReader(new InputStreamReader(new FileInputStream(path.toFile()))))) { // latest.log
            String content;
            LogEntry logEntry = null;
            while ((content = in.readLine()) != null) {
                Matcher logLineMatcher = LOG4J_PATTERN.matcher(content);
                if (logLineMatcher.matches()) { // current line is a new log entry
                    if (logEntry != null) {
                        // we had a previous log entry; analyze it!
                        LogEntry result = analyzeLogEntry(logEntry, searchTerm, matchCase, removeFormatting);
                        if (result != null) {
                            searchResults.add(result);
                            foundSearchTermInFile = true;
                        }
                        logEntry = null;
                    }
                    // handle first line of new log entry
                    if (chatOnly && logLineMatcher.group("isChat") == null) {
                        // not a chat log entry, although we're only searching for chat messages, abort!
                        continue;
                    }
                    LocalDateTime dateTime = getDate(date, logLineMatcher);
                    logEntry = new LogEntry(dateTime, path, logLineMatcher.group("message"));
                } else if (logEntry != null) {
                    // multiline log entry
                    logEntry.addLogLine(content);
                }
            }
            if (logEntry != null) {
                // end of file! analyze last log entry in file
                LogEntry result = analyzeLogEntry(logEntry, searchTerm, matchCase, removeFormatting);
                if (result != null) {
                    searchResults.add(result);
                    foundSearchTermInFile = true;
                }
            }
            if (foundSearchTermInFile) {
                analyzedFilesWithHits++;
            }
        } catch (IOException ignored) {
            // most likely corrupted .log.gz file - skip it.
        }
        return searchResults;
    }

    private LocalDateTime getDate(LocalDate date, Matcher logLineMatcher) {
        int hour = Integer.parseInt(logLineMatcher.group(1));
        int minute = Integer.parseInt(logLineMatcher.group(2));
        int sec = Integer.parseInt(logLineMatcher.group(3));

        return LocalDateTime.of(date, LocalTime.of(hour, minute, sec));
    }

    private LogEntry analyzeLogEntry(LogEntry logEntry, String searchTerms, boolean matchCase, boolean removeFormatting) {
        if (logEntry.getMessage().length() > 5000) {
            // avoid ultra long log entries
            return null;
        }
        logEntry.fixWeirdCharacters();

        if (removeFormatting) {
            logEntry.removeFormatting();
        }
        String logMessage = logEntry.getMessage();
        if (!matchCase) {
            if (!StringUtils.containsIgnoreCase(logMessage, searchTerms)) {
                // no result, abort
                return null;
            }
        } else if (!logMessage.contains(searchTerms)) {
            // no result, abort
            return null;
        }

        return logEntry;
    }
}
