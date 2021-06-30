package de.cowtipper.cowlection.util;

import com.mojang.realmsclient.util.Pair;
import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.config.MooConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Utils {
    public static final Pattern VALID_UUID_PATTERN = Pattern.compile("^(\\w{8})-(\\w{4})-(\\w{4})-(\\w{4})-(\\w{12})$");
    private static final Pattern VALID_USERNAME = Pattern.compile("^[\\w]{1,16}$");
    private static final NavigableMap<Double, Character> NUMBER_SUFFIXES = new TreeMap<>();
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.#", new DecimalFormatSymbols(Locale.ENGLISH));
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,##0", new DecimalFormatSymbols(Locale.ENGLISH));

    static {
        NUMBER_SUFFIXES.put(1_000d, 'k');
        NUMBER_SUFFIXES.put(1_000_000d, 'm');
        NUMBER_SUFFIXES.put(1_000_000_000d, 'b');
        NUMBER_SUFFIXES.put(1_000_000_000_000d, 't');
    }

    private Utils() {
    }

    public static boolean isValidUuid(String uuid) {
        return VALID_UUID_PATTERN.matcher(uuid).matches();
    }

    public static boolean isInvalidMcName(String username) {
        return !VALID_USERNAME.matcher(username).matches();
    }

    public static String fancyCase(String string) {
        return WordUtils.capitalizeFully(string.replace('_', ' '), ' ', '-');
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
            return DECIMAL_FORMAT.format(monthsPast) + " month" + (monthsPast >= 2 ? "s" : "");
        }
        double yearsPast = monthsPast / 12d;
        return DECIMAL_FORMAT.format(yearsPast) + " year" + (yearsPast >= 2 ? "s" : "");
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

    public static String formatDecimal(double number) {
        return DECIMAL_FORMAT.format(number);
    }

    public static String formatNumber(double number) {
        return NUMBER_FORMAT.format(number);
    }

    /**
     * Formats a large number with abbreviations for each factor of a thousand (k, m, b, t)
     */
    public static String formatNumberWithAbbreviations(double number) {
        if (number == Double.MIN_VALUE) return formatNumberWithAbbreviations(Double.MIN_VALUE + 1);
        if (number < 0) return "-" + formatNumberWithAbbreviations(-number);
        if (number < 1000) return "" + (long) number;

        Map.Entry<Double, Character> e = NUMBER_SUFFIXES.floorEntry(number);
        Double divideBy = e.getKey();
        Character suffix = e.getValue();

        String amountOfDecimals;
        switch (suffix) {
            case 'k':
                amountOfDecimals = "#";
                break;
            case 'm':
                amountOfDecimals = "##";
                break;
            default:
                amountOfDecimals = "###";
        }
        DecimalFormat df = new DecimalFormat("#,##0." + amountOfDecimals, new DecimalFormatSymbols(Locale.ENGLISH));
        return df.format(number / divideBy) + suffix;
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
            default:
                return "";
        }
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
            case 12:
                tierColor = EnumChatFormatting.DARK_AQUA;
                break;
            default:
                tierColor = EnumChatFormatting.OBFUSCATED;
        }
        return tierColor;
    }


    public static String booleanToSymbol(boolean value) {
        return value ? EnumChatFormatting.GREEN + "✔" : EnumChatFormatting.RED + "✘";
    }

    public static <V> Map<String, V> getLastNMapEntries(Map<String, V> map, int numberOfElements) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        if (map.size() <= numberOfElements) {
            return map;
        }
        return map.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                .limit(numberOfElements)
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    public static Pair<String, String> extractSbItemBaseName(String originalItemName, NBTTagCompound extraAttributes, boolean strikethrough) {
        String reforge = "";
        StringBuilder modifiedItemName = new StringBuilder(originalItemName);
        String grayedOutFormatting = "" + EnumChatFormatting.GRAY + EnumChatFormatting.STRIKETHROUGH;

        if (extraAttributes.hasKey("modifier")) {
            // item has been reforged; re-format item name to exclude reforges
            reforge = StringUtils.capitalize(extraAttributes.getString("modifier"));
            int modifierSuffix = Math.max(reforge.indexOf("_sword"), reforge.indexOf("_bow"));
            if (modifierSuffix != -1) {
                reforge = reforge.substring(0, modifierSuffix);
            }
            int reforgeInItemName = originalItemName.indexOf(reforge);
            if (reforgeInItemName == -1 && reforge.equals("Light") && extraAttributes.getString("id").startsWith("HEAVY_")) {
                // special case: heavy armor with light reforge
                reforgeInItemName = originalItemName.indexOf("Heavy");
            }

            if (reforgeInItemName > 0 && !originalItemName.contains(EnumChatFormatting.STRIKETHROUGH.toString())) {
                // we have a reforged item! remove reforge in item name and remove any essence upgrades (✪)

                int reforgeLength = reforge.length();
                String reforgePrefix = null;
                // special cases for reforge + item name
                if (reforge.equals("Heavy") && extraAttributes.getString("id").startsWith("HEAVY_")) {
                    reforgePrefix = "Extremely ";
                } else if (reforge.equals("Light") && extraAttributes.getString("id").startsWith("HEAVY_")) {
                    reforgePrefix = "Not So ";
                } else if ((reforge.equals("Wise") && extraAttributes.getString("id").startsWith("WISE_DRAGON_"))
                        || (reforge.equals("Strong") && extraAttributes.getString("id").startsWith("STRONG_DRAGON_"))) {
                    reforgePrefix = "Very ";
                } else if (reforge.equals("Superior") && extraAttributes.getString("id").startsWith("SUPERIOR_DRAGON_")) {
                    reforgePrefix = "Highly ";
                } else if (reforge.equals("Perfect") && extraAttributes.getString("id").startsWith("PERFECT_")) {
                    reforgePrefix = "Absolutely ";
                }
                if (reforgePrefix != null) {
                    reforgeInItemName -= reforgePrefix.length();
                    reforgeLength = reforgePrefix.length() - 1;
                }

                if (strikethrough) {
                    modifiedItemName.insert(reforgeInItemName, grayedOutFormatting)
                            .insert(reforgeInItemName + reforgeLength + grayedOutFormatting.length(), originalItemName.substring(0, reforgeInItemName));
                } else {
                    modifiedItemName.delete(reforgeInItemName, reforgeInItemName + reforgeLength);
                }
            }
        }
        // remove or 'hide' essence upgrade indicators (✪)
        replaceInStringBuilder(modifiedItemName, EnumChatFormatting.GOLD + "✪", grayedOutFormatting + "✪", strikethrough);
        replaceInStringBuilder(modifiedItemName, EnumChatFormatting.RED + "✪", "" + EnumChatFormatting.DARK_GRAY + EnumChatFormatting.STRIKETHROUGH + "✪", strikethrough);

        return Pair.of(modifiedItemName.toString().trim(), reforge);
    }

    private static void replaceInStringBuilder(StringBuilder sb, String search, String replacement, boolean replace) {
        int hit = sb.indexOf(search);
        while (hit > 0) {
            if (replace) {
                sb.replace(hit, hit + search.length(), replacement);
            } else {
                sb.delete(hit, hit + search.length());
            }
            hit = sb.indexOf(search);
        }
    }

    public static void copyToClipboardOrSaveAsFile(String what, String fileName, NBTBase data, boolean sortData) {
        String nbt = GsonUtils.toJson(data, sortData);
        if (MooConfig.copyWailaAndInventoryDataToClipboard()) {
            GuiScreen.setClipboardString(nbt);
            Cowlection.getInstance().getChatHelper().sendMessage(EnumChatFormatting.GREEN, "Copied " + what + " to clipboard.");
        } else {
            try {
                File target = getTimestampedFileForDirectory(fileName, "json");
                if (target == null) {
                    return;
                }
                Files.write(target.toPath(), nbt.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);

                File targetNormalized = target.getCanonicalFile();

                Cowlection.getInstance().getChatHelper().sendMessage(new MooChatComponent("Saved " + what).green()
                        .appendSibling(new MooChatComponent(" [open file]").gold().setOpenFile(targetNormalized))
                        .appendSibling(new MooChatComponent(" [open folder]").darkAqua().setOpenFile(targetNormalized.getParentFile())));
            } catch (IOException | UnsupportedOperationException e) {
                e.printStackTrace();
                Cowlection.getInstance().getChatHelper().sendMessage(EnumChatFormatting.RED, "Couldn't save " + what + ": " + e.toString());
            }
        }
    }

    /**
     * Based on ScreenShotHelper#getTimestampedPNGFileForDirectory
     */
    static File getTimestampedFileForDirectory(String suffix, String fileType) {
        File cowlectionOutPath = new File(Minecraft.getMinecraft().mcDataDir, Cowlection.MODID.toLowerCase() + "_out");
        if (!cowlectionOutPath.exists() && !cowlectionOutPath.mkdirs()) {
            // dir didn't exist and couldn't be created
            return null;
        }

        String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss"));
        String sanitizedSuffix = StringUtils.replaceEach(EnumChatFormatting.getTextWithoutFormattingCodes(suffix).trim(),
                // replacement characters from https://stackoverflow.com/a/61448658
                new String[]{"\\", "/", ":", "*", "?", "\"", "<", ">", "|"},
                new String[]{"⧵", "∕", "∶", "∗", "？", "“", "‹", "›", "∣"});
        String baseName = currentDateTime + "_" + sanitizedSuffix;
        int i = 1;
        while (true) {
            File timestampedFile = new File(cowlectionOutPath, baseName + (i == 1 ? "" : "_" + i) + "." + fileType);
            if (!timestampedFile.exists()) {
                return timestampedFile;
            }
            ++i;
        }
    }
}
