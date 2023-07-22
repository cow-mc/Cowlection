package de.cowtipper.cowlection.error;

import net.minecraftforge.fml.common.eventhandler.Event;

public class ApiHttpErrorEvent extends Event {
    private final String message;
    private final String url;
    private final boolean wasUsingApiKey;

    public ApiHttpErrorEvent(String message, String url, boolean wasUsingApiKey) {
        this.message = message;
        this.url = url;
        this.wasUsingApiKey = wasUsingApiKey;
    }

    public String getMessage() {
        return message;
    }

    public String getUrl() {
        return url;
    }

    public String getBaseUrl() {
        int queryParamStart = url.indexOf('?', 10);
        return queryParamStart > 0 ? url.substring(0, queryParamStart) : url;
    }

    public boolean wasUsingApiKey() {
        return wasUsingApiKey;
    }
}
