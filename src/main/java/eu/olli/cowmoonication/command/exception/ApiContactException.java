package eu.olli.cowmoonication.command.exception;

public class ApiContactException extends MooCommandException {
    public ApiContactException(String api, String failedAction) {
        super("Sorry, couldn't contact the " + api + " API and thus " + failedAction);
    }
}
