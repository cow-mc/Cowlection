package de.cowtipper.cowlection.error;

import java.io.IOException;

public class ApiHttpErrorException extends IOException {
    private final String url;

    public ApiHttpErrorException(String message, String url) {
        super(message);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
