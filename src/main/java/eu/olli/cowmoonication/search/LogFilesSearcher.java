package eu.olli.cowmoonication.search;

import eu.olli.cowmoonication.config.MooConfig;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

class LogFilesSearcher {
    private static final Pattern UTF_PARAGRAPH_SYMBOL = Pattern.compile("Â§");

    List<String> searchFor(String searchQuery, boolean matchCase, boolean removeFormatting, LocalDate dateStart, LocalDate dateEnd) {
        List<String> files = new ArrayList<>();
        for (String logsDirPath : MooConfig.logsDirs) {
            File logsDir = new File(logsDirPath);
            if (logsDir.exists() && logsDir.isDirectory()) {
                try {
                    files.addAll(fileList(logsDir, dateStart, dateEnd));
                } catch (IOException e) {
                    return printErrors(logsDirPath, e);
                }
            }
        }

        if (files.isEmpty()) {
            List<String> errors = new ArrayList<>();
            errors.add(EnumChatFormatting.DARK_RED + "ERROR: Couldn't find any Minecraft log files. Please check if the log file directories are set correctly (/moo config).");
            return errors;
        } else {
            return analyseFiles(files, searchQuery, matchCase, removeFormatting);
        }
    }

    private List<String> analyseFiles(List<String> files, String searchTerm, boolean matchCase, boolean removeFormatting) {
        List<String> searchResults = new ArrayList<>();
        for (String file : files) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))))) {
                String content;
                while ((content = in.readLine()) != null) {
                    String result = analyseLine(content, searchTerm, matchCase);
                    if (result != null) {
                        if (result.contains("Â§")) {
                            result = UTF_PARAGRAPH_SYMBOL.matcher(result).replaceAll("§");
                        }
                        if (removeFormatting) {
                            result = EnumChatFormatting.getTextWithoutFormattingCodes(result);
                        }
                        String date = file.substring(file.lastIndexOf('\\') + 1, file.lastIndexOf('-'));
                        searchResults.add(EnumChatFormatting.DARK_GRAY + date + " " + EnumChatFormatting.RESET + result);
                    }
                }
            } catch (IOException e) {
                return printErrors(file, e);
            }
        }
        return searchResults;
    }

    private String analyseLine(String logLine, String searchTerms, boolean matchCase) {
        String result = logLine;
        if (!matchCase) {
            logLine = logLine.toLowerCase();
            searchTerms = searchTerms.toLowerCase();
        }
        return logLine.contains(searchTerms) ? (result.contains("[Client thread/INFO]: [CHAT]") ? result.substring(40) : result) : null;
    }

    private List<String> fileList(File directory, LocalDate startDate, LocalDate endDate) throws IOException {
        List<String> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory.toPath())) {
            for (Path path : directoryStream) {
                if (path.toString().endsWith(".log.gz")) {
                    String[] fileDate = path.getFileName().toString().split("-");
                    if (fileDate.length == 4) {
                        LocalDate fileLocalDate = LocalDate.of(Integer.parseInt(fileDate[0]),
                                Integer.parseInt(fileDate[1]), Integer.parseInt(fileDate[2]));

                        if (fileLocalDate.compareTo(startDate) >= 0 && fileLocalDate.compareTo(endDate) <= 0) {
                            fileNames.add(path.toString());
                        }
                    } else {
                        System.err.println("Error with " + path.toString());
                    }
                }
            }
        }
        return fileNames;
    }

    private List<String> printErrors(String file, IOException e) {
        System.err.println("Error reading/parsing file: " + file);
        e.printStackTrace();
        List<String> errorMessage = new ArrayList<>();
        errorMessage.add(EnumChatFormatting.DARK_RED + "ERROR: An error occurred trying to read/parse '" + EnumChatFormatting.RED + file + EnumChatFormatting.DARK_RED + "'");
        errorMessage.add(StringUtils.replaceEach(ExceptionUtils.getStackTrace(e), new String[]{"\t", "\r\n"}, new String[]{"  ", "\n"}));
        return errorMessage;
    }
}
