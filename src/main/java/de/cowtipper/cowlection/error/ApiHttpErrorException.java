package de.cowtipper.cowlection.error;

import java.io.IOException;

public class ApiHttpErrorException extends IOException {
    private final String url;
    private final boolean hasApiKey;

    public ApiHttpErrorException(String message, String url) {
        this(message, url, false);
    }

    public ApiHttpErrorException(String message, String url, boolean hasApiKey) {
        super(message);
        this.url = url;
        this.hasApiKey = hasApiKey;
    }

    public String getUrl() {
        return url;
    }

    public boolean wasUsingApiKey() {
        return hasApiKey;
    }
}
