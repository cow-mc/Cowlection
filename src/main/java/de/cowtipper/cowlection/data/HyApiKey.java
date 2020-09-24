package de.cowtipper.cowlection.data;

public class HyApiKey {
    private boolean success;
    private String cause;

    /**
     * No-args constructor for GSON
     */
    private HyApiKey() {
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCause() {
        return cause;
    }
}
