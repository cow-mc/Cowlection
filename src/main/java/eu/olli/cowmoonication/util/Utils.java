package eu.olli.cowmoonication.util;

import org.apache.commons.lang3.text.WordUtils;

import java.util.regex.Pattern;

public final class Utils {
    public static final Pattern VALID_UUID_PATTERN = Pattern.compile("^(\\w{8})-(\\w{4})-(\\w{4})-(\\w{4})-(\\w{12})$");
    private static final Pattern VALID_USERNAME = Pattern.compile("^[\\w]{1,16}$");

    private Utils() {
    }

    public static boolean isValidUuid(String uuid) {
        return VALID_UUID_PATTERN.matcher(uuid).matches();
    }

    public static boolean isValidMcName(String username) {
        return VALID_USERNAME.matcher(username).matches();
    }

    public static String fancyCase(String string) {
        return WordUtils.capitalizeFully(string.replace('_', ' '));
    }
}
