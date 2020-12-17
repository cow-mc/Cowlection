package de.cowtipper.cowlection.data;

public class HyApiKey {
    @SuppressWarnings("unused")
    private boolean success;
    @SuppressWarnings("unused")
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
