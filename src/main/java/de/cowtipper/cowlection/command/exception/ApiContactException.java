package de.cowtipper.cowlection.command.exception;

import de.cowtipper.cowlection.Cowlection;

public class ApiContactException extends MooCommandException {
    public ApiContactException(String api, String failedAction) {
        super("Sorry, couldn't contact the " + api + " API and thus " + failedAction);
        if (api.equals("Hypixel") && failedAction.contains("Invalid API key")) {
            Cowlection.getInstance().getMoo().setMooValidity(false);
        }
    }
}
