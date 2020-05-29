package eu.olli.cowmoonication.util;

import com.mojang.realmsclient.util.Pair;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class Utils {
    public static final Pattern VALID_UUID_PATTERN = Pattern.compile("^(\\w{8})-(\\w{4})-(\\w{4})-(\\w{4})-(\\w{12})$");
    private static final Pattern VALID_USERNAME = Pattern.compile("^[\\w]{1,16}$");
    private static final char[] LARGE_NUMBERS = new char[]{'k', 'm', 'b', 't'};

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

    /**
     * Turn timestamp into pretty-formatted duration and date details.
     *
     * @param timestamp last login/logout
     * @return 1st: duration between timestamp and now in words; 2nd: formatted date if time differences is >24h, otherwise null
     */
    public static Pair<String, String> getLastOnlineWords(long timestamp) {
        long duration = System.currentTimeMillis() - timestamp;
        long daysPast = TimeUnit.MILLISECONDS.toDays(duration);

        String dateFormatted = null;
        if (daysPast > 1) {
            dateFormatted = DateFormatUtils.format(timestamp, "dd-MMM-yyyy");
        }

        if (daysPast > 31) {
            return Pair.of(
                    DurationFormatUtils.formatPeriod(timestamp, System.currentTimeMillis(), (daysPast > 365 ? "y 'years' " : "") + "M 'months' d 'days'"),
                    dateFormatted);
        } else {
            return Pair.of(
                    DurationFormatUtils.formatDurationWords(duration, true, true),
                    dateFormatted);
        }
    }

    /**
     * Formats a large number with abbreviations for each factor of a thousand (k, m, ...)
     *
     * @param number the number to format
     * @return a String representing the number n formatted in a cool looking way.
     * @see <a href="https://stackoverflow.com/a/4753866">Source</a>
     */
    public static String formatNumberWithAbbreviations(double number) {
        return formatNumberWithAbbreviations(number, 0);
    }

    private static String formatNumberWithAbbreviations(double number, int iteration) {
        @SuppressWarnings("IntegerDivisionInFloatingPointContext") double d = ((long) number / 100) / 10.0;
        boolean isRound = (d * 10) % 10 == 0; //true if the decimal part is equal to 0 (then it's trimmed anyway)
        // this determines the class, i.e. 'k', 'm' etc
        // this decides whether to trim the decimals
        // (int) d * 10 / 10 drops the decimal
        return d < 1000 ? // this determines the class, i.e. 'k', 'm' etc
                (d > 99.9 || isRound || d > 9.99 ? // this decides whether to trim the decimals
                        (int) d * 10 / 10 : d + "" // (int) d * 10 / 10 drops the decimal
                ) + "" + LARGE_NUMBERS[iteration]
                : formatNumberWithAbbreviations(d, iteration + 1);
    }
}
