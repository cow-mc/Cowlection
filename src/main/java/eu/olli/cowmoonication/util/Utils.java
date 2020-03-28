package eu.olli.cowmoonication.util;

import java.util.regex.Pattern;

public final class Utils {
    private static final Pattern VALID_USERNAME = Pattern.compile("^[\\w]{1,16}$");

    private Utils() {
    }

    public static boolean isValidMcName(String username) {
        return VALID_USERNAME.matcher(username).matches();
    }
}
