package eu.olli.cowlection.search;

import eu.olli.cowlection.config.MooConfig;
import eu.olli.cowlection.data.LogEntry;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

class LogFilesSearcher {
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
        List<Path> files = new ArrayList<>();
        for (String logsDirPath : MooConfig.logsDirs) {
            File logsDir = new File(logsDirPath);
            if (logsDir.exists() && logsDir.isDirectory()) {
                try {
                    files.addAll(fileList(logsDir, dateStart, dateEnd));
                } catch (IOException e) {
                    throw throwIoException(logsDirPath, e);
                }
            }
        }

        if (files.isEmpty()) {
            throw new FileNotFoundException(EnumChatFormatting.DARK_RED + "ERROR: Couldn't find any Minecraft log files. Please check if the log file directories are set correctly (/moo config).");
        } else {
            List<LogEntry> searchResults = analyzeFiles(files, searchQuery, chatOnly, matchCase, removeFormatting)
                    .stream().sorted(Comparator.comparing(LogEntry::getTime)).collect(Collectors.toList());
            return new ImmutableTriple<>(files.size(), analyzedFilesWithHits, searchResults);
        }
    }

    private List<LogEntry> analyzeFiles(List<Path> paths, String searchTerm, boolean chatOnly, boolean matchCase, boolean removeFormatting) throws IOException {
        List<LogEntry> searchResults = new ArrayList<>();
        for (Path path : paths) {
            boolean foundSearchTermInFile = false;
            try (BufferedReader in = (path.endsWith("latest.log")
                    ? new BufferedReader(new InputStreamReader(new FileInputStream(path.toFile()))) // latest.log
                    : new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(path.toFile())))))) { // ....log.gz
                String fileName = path.getFileName().toString(); // 2020-04-20-3.log.gz
                String date = fileName.equals("latest.log")
                        ? LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                        : fileName.substring(0, fileName.lastIndexOf('-'));
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
            } catch (IOException e) {
                throw throwIoException(path.toString(), e);
            }
        }
        return searchResults;
    }

    private LocalDateTime getDate(String date, Matcher logLineMatcher) {
        int year = Integer.parseInt(date.substring(0, 4));
        int month = Integer.parseInt(date.substring(5, 7));
        int day = Integer.parseInt(date.substring(8, 10));
        int hour = Integer.parseInt(logLineMatcher.group(1));
        int minute = Integer.parseInt(logLineMatcher.group(2));
        int sec = Integer.parseInt(logLineMatcher.group(3));

        return LocalDateTime.of(year, month, day, hour, minute, sec);
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

    private List<Path> fileList(File directory, LocalDate startDate, LocalDate endDate) throws IOException {
        List<Path> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory.toPath())) {
            for (Path path : directoryStream) {
                if (path.toString().endsWith(".log.gz")) {
                    String[] fileDate = path.getFileName().toString().split("-");
                    if (fileDate.length == 4) {
                        LocalDate fileLocalDate = LocalDate.of(Integer.parseInt(fileDate[0]),
                                Integer.parseInt(fileDate[1]), Integer.parseInt(fileDate[2]));

                        if (fileLocalDate.compareTo(startDate) >= 0 && fileLocalDate.compareTo(endDate) <= 0) {
                            fileNames.add(path);
                        }
                    } else {
                        System.err.println("Error with " + path.toString());
                    }
                } else if (path.getFileName().toString().equals("latest.log")) {
                    LocalDate lastModified = Instant.ofEpochMilli(path.toFile().lastModified()).atZone(ZoneId.systemDefault()).toLocalDate();
                    if (!lastModified.isBefore(startDate) && !lastModified.isAfter(endDate)) {
                        fileNames.add(path);
                    }
                }
            }
        }
        return fileNames;
    }

    private IOException throwIoException(String file, IOException e) throws IOException {
        IOException ioException = new IOException(EnumChatFormatting.DARK_RED + "ERROR: An error occurred trying to read/parse '" + EnumChatFormatting.RED + file + EnumChatFormatting.DARK_RED + "'");
        ioException.setStackTrace(e.getStackTrace());
        throw ioException;
    }
}
