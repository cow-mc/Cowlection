package de.cowtipper.cowlection.data;

import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

public class LogEntry {
    private static final Pattern UTF_PARAGRAPH_SYMBOL = Pattern.compile("Â§");
    private LocalDateTime time;
    private Path filePath;
    private String message;

    public LogEntry(LocalDateTime time, Path filePath, String logEntry) {
        this.time = time;
        this.filePath = filePath;
        this.message = logEntry;
    }

    public LogEntry(String message) {
        this.message = message;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public Path getFilePath() {
        return filePath;
    }

    public String getMessage() {
        return message;
    }

    public void addLogLine(String logLine) {
        message += "\n" + logLine;
    }

    public void removeFormatting() {
        this.message = EnumChatFormatting.getTextWithoutFormattingCodes(message);
    }

    public void fixWeirdCharacters() {
        if (message.contains("Â§")) {
            message = UTF_PARAGRAPH_SYMBOL.matcher(message).replaceAll("§");
        }
    }

    /**
     * Is this log entry a 'real' log entry or just an error message from the search process?
     *
     * @return true if error message, otherwise false
     */
    public boolean isError() {
        return time == null && filePath == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogEntry logEntry = (LogEntry) o;
        return new EqualsBuilder()
                .append(time, logEntry.time)
                .append(filePath, logEntry.filePath)
                .append(message, logEntry.message)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(time)
                .append(filePath)
                .append(message)
                .toHashCode();
    }
}
