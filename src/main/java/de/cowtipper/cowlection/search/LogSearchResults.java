package de.cowtipper.cowlection.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LogSearchResults {
    private final AtomicInteger analyzedFiles;
    private final AtomicInteger skippedFiles;
    private final List<LogEntry> searchResults;
    private int analyzedFilesWithHits;

    public LogSearchResults() {
        this.analyzedFiles = new AtomicInteger();
        this.skippedFiles = new AtomicInteger();
        this.searchResults = Collections.synchronizedList(new ArrayList<>());
    }

    public void addAnalyzedFile() {
        analyzedFiles.incrementAndGet();
    }

    public void addSkippedFile() {
        skippedFiles.incrementAndGet();
    }

    public void addSearchResults(List<LogEntry> newResults) {
        searchResults.addAll(newResults);
    }

    public void setAnalyzedFilesWithHits(int analyzedFilesWithHits) {
        this.analyzedFilesWithHits = analyzedFilesWithHits;
    }

    public int getAnalyzedFiles() {
        return analyzedFiles.get();
    }

    public int getSkippedFiles() {
        return skippedFiles.get();
    }

    public List<LogEntry> getSortedSearchResults() {
        return searchResults.stream().sorted(Comparator.comparing(LogEntry::getTime)).collect(Collectors.toList());
    }

    public int getAnalyzedFilesWithHits() {
        return analyzedFilesWithHits;
    }
}
