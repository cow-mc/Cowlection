package de.cowtipper.cowlection.search;

import com.mojang.realmsclient.util.Pair;
import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.data.LogEntry;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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

    ImmutableTriple<Integer, Integer, List<LogEntry>> searchFor(String searchQuery, boolean chatOnly, boolean matchCase, boolean removeFormatting, LocalDate dateStart, LocalDate dateEnd) throws IOException {
        List<Pair<Path, LocalDate>> files = new ArrayList<>();
        for (String logsDirPath : MooConfig.logsDirs) {
            File logsDir = new File(logsDirPath);
            if (logsDir.exists() && logsDir.isDirectory()) {
                try {
                    files.addAll(fileList(logsDir, dateStart, dateEnd));
                } catch (IOException e) {
                    throw new IOException(EnumChatFormatting.DARK_RED + "ERROR: An error occurred trying to read/parse '" + EnumChatFormatting.RED + logsDirPath + EnumChatFormatting.DARK_RED + "':\n"
                            + EnumChatFormatting.GOLD + e.getLocalizedMessage(), e);
                }
            }
        }

        if (files.isEmpty()) {
            throw new FileNotFoundException(EnumChatFormatting.DARK_RED + "ERROR: No Minecraft log files could be found for the selected date range. Please check if the dates as well as the directories of the log files are set correctly (Log Search ➡ Settings).");
        } else {
            List<LogEntry> searchResults = analyzeFiles(files, searchQuery, chatOnly, matchCase, removeFormatting)
                    .stream().sorted(Comparator.comparing(LogEntry::getTime)).collect(Collectors.toList());
            return new ImmutableTriple<>(files.size(), analyzedFilesWithHits, searchResults);
        }
    }

    private List<LogEntry> analyzeFiles(List<Pair<Path, LocalDate>> pathsData, String searchTerm, boolean chatOnly, boolean matchCase, boolean removeFormatting) {
        List<LogEntry> searchResults = new ArrayList<>();
        for (Pair<Path, LocalDate> pathData : pathsData) {
            Path path = pathData.first();
            boolean foundSearchTermInFile = false;
            try (BufferedReader in = (path.endsWith("latest.log")
                    ? new BufferedReader(new InputStreamReader(new FileInputStream(path.toFile()))) // latest.log
                    : new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(path.toFile())))))) { // ....log.gz
                LocalDate date = pathData.second();
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

    private List<Pair<Path, LocalDate>> fileList(File directory, LocalDate startDate, LocalDate endDate) throws IOException {
        List<Pair<Path, LocalDate>> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory.toPath())) {
            for (Path path : directoryStream) {
                if (path.toString().endsWith(".log.gz")) {
                    Matcher fileNameMatcher = LOG_FILE_PATTERN.matcher(path.getFileName().toString());
                    if (fileNameMatcher.matches()) {
                        LocalDate fileLocalDate = LocalDate.of(Integer.parseInt(fileNameMatcher.group(1)),
                                Integer.parseInt(fileNameMatcher.group(2)), Integer.parseInt(fileNameMatcher.group(3)));
                        if (!fileLocalDate.isBefore(startDate) && !fileLocalDate.isAfter(endDate)) {
                            fileNames.add(Pair.of(path, fileLocalDate));
                        }
                    }
                } else if (path.getFileName().toString().equals("latest.log")) {
                    LocalDate lastModified = Instant.ofEpochMilli(path.toFile().lastModified()).atZone(ZoneId.systemDefault()).toLocalDate();
                    if (!lastModified.isBefore(startDate) && !lastModified.isAfter(endDate)) {
                        fileNames.add(Pair.of(path, lastModified));
                    }
                }
            }
        }
        return fileNames;
    }
}
