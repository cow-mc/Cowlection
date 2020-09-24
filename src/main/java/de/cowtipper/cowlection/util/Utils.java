package de.cowtipper.cowlection.util;

import com.mojang.realmsclient.util.Pair;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
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
    public static Pair<String, String> getDurationAsWords(long timestamp) {
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

    public static String getDurationAsWord(long timestamp) {
        long duration = System.currentTimeMillis() - timestamp;
        long secondsPast = TimeUnit.MILLISECONDS.toSeconds(duration);
        if (secondsPast < 60) {
            return secondsPast + " second" + (secondsPast > 1 ? "s" : "");
        }
        long minutesPast = TimeUnit.SECONDS.toMinutes(secondsPast);
        if (minutesPast < 60) {
            return minutesPast + " minute" + (minutesPast > 1 ? "s" : "");
        }
        long hoursPast = TimeUnit.MINUTES.toHours(minutesPast);
        if (hoursPast < 24) {
            return hoursPast + " hour" + (hoursPast > 1 ? "s" : "");
        }
        long daysPast = TimeUnit.HOURS.toDays(hoursPast);
        if (daysPast < 31) {
            return daysPast + " day" + (daysPast > 1 ? "s" : "");
        }
        double monthsPast = daysPast / 30.5d;
        if (monthsPast < 12) {
            return new DecimalFormat("0.#").format(monthsPast) + " month" + (monthsPast >= 2 ? "s" : "");
        }
        double yearsPast = monthsPast / 12d;
        return new DecimalFormat("0.#").format(yearsPast) + " year" + (yearsPast >= 2 ? "s" : "");
    }

    public static String toRealPath(Path path) {
        try {
            return path.toRealPath().toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "file not found";
        }
    }

    public static String toRealPath(File path) {
        return toRealPath(path.toPath());
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
        boolean isRound = (d * 10) % 10 == 0; // true if the decimal part is equal to 0 (then it's trimmed anyway)
        // this determines the class, i.e. 'k', 'm' etc
        // this decides whether to trim the decimals
        // (int) d * 10 / 10 drops the decimal
        return d < 1000 ? // this determines the class, i.e. 'k', 'm' etc
                (d > 99.9 || isRound || d > 9.99 ? // this decides whether to trim the decimals
                        (int) d * 10 / 10 : d + "" // (int) d * 10 / 10 drops the decimal
                ) + "" + LARGE_NUMBERS[iteration]
                : formatNumberWithAbbreviations(d, iteration + 1);
    }

    /**
     * Convert Roman numerals to their corresponding Arabic numeral
     *
     * @param roman Roman numeral
     * @return Arabic numeral
     * @see <a href="https://www.w3resource.com/javascript-exercises/javascript-math-exercise-22.php">Source</a>
     */
    public static int convertRomanToArabic(String roman) {
        if (roman == null) return -1;
        int number = romanCharToArabic(roman.charAt(0));

        for (int i = 1; i < roman.length(); i++) {
            int current = romanCharToArabic(roman.charAt(i));
            int previous = romanCharToArabic(roman.charAt(i - 1));
            if (current <= previous) {
                number += current;
            } else {
                number = number - previous * 2 + current;
            }
        }
        return number;
    }

    private static int romanCharToArabic(char c) {
        switch (c) {
            case 'I':
                return 1;
            case 'V':
                return 5;
            case 'X':
                return 10;
            case 'L':
                return 50;
            case 'C':
                return 100;
            case 'D':
                return 500;
            case 'M':
                return 1000;
            default:
                return -1;
        }
    }

    /**
     * Convert Arabic numerals to their corresponding Roman numerals
     *
     * @param number Arabic numerals
     * @return Roman numerals
     * @see <a href="https://stackoverflow.com/a/48357180">Source</a>
     */
    public static String convertArabicToRoman(int number) {
        if (number == 0) {
            return "0";
        }
        String romanOnes = arabicToRomanChars(number % 10, "I", "V", "X");
        number /= 10;

        String romanTens = arabicToRomanChars(number % 10, "X", "L", "C");
        number /= 10;

        String romanHundreds = arabicToRomanChars(number % 10, "C", "D", "M");
        number /= 10;

        String romanThousands = arabicToRomanChars(number % 10, "M", "", "");

        return romanThousands + romanHundreds + romanTens + romanOnes;
    }

    private static String arabicToRomanChars(int n, String one, String five, String ten) {
        switch (n) {
            case 1:
                return one;
            case 2:
                return one + one;
            case 3:
                return one + one + one;
            case 4:
                return one + five;
            case 5:
                return five;
            case 6:
                return five + one;
            case 7:
                return five + one + one;
            case 8:
                return five + one + one + one;
            case 9:
                return one + ten;
        }
        return "";
    }

    /**
     * Get the minion tier's color for chat formatting
     *
     * @param tier minion tier
     * @return color code corresponding to the tier
     */
    public static EnumChatFormatting getMinionTierColor(int tier) {
        EnumChatFormatting tierColor;
        switch (tier) {
            case 1:
                tierColor = EnumChatFormatting.WHITE;
                break;
            case 2:
            case 3:
            case 4:
                tierColor = EnumChatFormatting.GREEN;
                break;
            case 5:
            case 6:
            case 7:
                tierColor = EnumChatFormatting.DARK_PURPLE;
                break;
            case 8:
            case 9:
            case 10:
                tierColor = EnumChatFormatting.RED;
                break;
            case 11:
                tierColor = EnumChatFormatting.AQUA;
                break;
            default:
                tierColor = EnumChatFormatting.OBFUSCATED;
        }
        return tierColor;
    }
}
