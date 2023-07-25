package de.cowtipper.cowlection.data;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.StringUtils;

import java.text.NumberFormat;
import java.util.Locale;

public class BestiaryEntry {
    public static ItemStack triggerItem;

    public static int highestKillsToGo;
    public static int highestKillGoal;
    public static int highestPercentageToGo;
    public static int widestMobNameWidth;

    private static int widestKillsToGoWidth;
    private static int widestKillGoalToGoWidth;
    private static int widestPercentageToGoWidth;

    private static final NumberFormat numberFormatter;
    private static final char dashSpacerChar;
    private static final char spacerChar;
    private static int dashSpacerWidth;
    private static int spacerWidth;

    private final String mobName;
    private final int mobNameWidth;
    private final int killsToGo;
    private final int killsGoal;
    private final int percentageToGo;

    static {
        numberFormatter = NumberFormat.getNumberInstance(Locale.US);
        numberFormatter.setMaximumFractionDigits(0);
        dashSpacerChar = '·';
        spacerChar = ' ';
    }

    private boolean isMaxed;

    public BestiaryEntry(String mobName, int currentKills, int killsGoal) {
        this.mobName = mobName;
        this.killsToGo = killsGoal - currentKills;
        this.killsGoal = killsGoal;
        this.percentageToGo = 100 - (currentKills * 100 / killsGoal);
        if (killsToGo > highestKillsToGo) {
            highestKillsToGo = killsToGo;
        }
        if (killsGoal > highestKillGoal) {
            highestKillGoal = killsGoal;
        }
        if (percentageToGo > highestPercentageToGo) {
            highestPercentageToGo = percentageToGo;
        }
        this.mobNameWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(mobName);
        if (mobNameWidth > widestMobNameWidth) {
            widestMobNameWidth = mobNameWidth;
        }
    }

    /**
     * Bestiary is maxed (when isMaxed=true), or has not been unlocked yet (when isMaxed=false)
     */
    public BestiaryEntry(String mobName, boolean isMaxed) {
        this.mobName = mobName;
        mobNameWidth = -1;
        killsToGo = Integer.MAX_VALUE - (isMaxed ? 0 : 1);
        killsGoal = -1;
        percentageToGo = Integer.MAX_VALUE - (isMaxed ? 0 : 1);
        this.isMaxed = isMaxed;
    }

    public static void reinitialize(ItemStack triggerItem) {
        BestiaryEntry.triggerItem = triggerItem;

        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        dashSpacerWidth = fontRenderer.getCharWidth(dashSpacerChar);
        spacerWidth = fontRenderer.getCharWidth(spacerChar);

        highestKillsToGo = 0;
        highestKillGoal = 0;
        highestPercentageToGo = 0;
        widestMobNameWidth = 0;
    }

    public static void calculateWidestEntries() {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        widestKillsToGoWidth = fontRenderer.getStringWidth(numberFormatter.format(highestKillsToGo));
        widestKillGoalToGoWidth = fontRenderer.getStringWidth(numberFormatter.format(highestKillGoal));
        widestPercentageToGoWidth = fontRenderer.getStringWidth(numberFormatter.format(highestPercentageToGo));
    }

    public static boolean isDifferentTriggerItem(ItemStack triggerItem) {
        return BestiaryEntry.triggerItem == null || !BestiaryEntry.triggerItem.getIsItemStackEqual(triggerItem);
    }

    public String getFormattedOutput(boolean sortBestiaryOverviewByKills) {
        if (isMaxed) {
            return mobName + EnumChatFormatting.WHITE + ": maxed!";
        }
        if (percentageToGo == Integer.MAX_VALUE - 1) {
            return mobName + EnumChatFormatting.GRAY + ": not unlocked yet";
        }

        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        StringBuilder currentEntry = new StringBuilder();
        String formattedKillsToGo = numberFormatter.format(killsToGo);
        String formattedKillsGoal = numberFormatter.format(killsGoal);
        String formattedPercentageToGo = numberFormatter.format(percentageToGo);

        int gapSize = (widestMobNameWidth - mobNameWidth) + (sortBestiaryOverviewByKills
                ? widestKillsToGoWidth - fontRenderer.getStringWidth(formattedKillsToGo)
                : widestPercentageToGoWidth - fontRenderer.getStringWidth(formattedPercentageToGo));
        int amountOfSpacerChars = Math.max(gapSize, 0) / dashSpacerWidth;

        currentEntry.append(mobName).append(EnumChatFormatting.RESET).append(EnumChatFormatting.DARK_GRAY).append(StringUtils.repeat(dashSpacerChar, amountOfSpacerChars)).append(' ');
        int remainingGap = gapSize % dashSpacerWidth;
        if (remainingGap >= 3) {
            // quite a large gap left = add another smaller spacer
            currentEntry.append(spacerChar);
            remainingGap -= spacerWidth;
        }

        StringBuilder killsInfo = new StringBuilder().append(sortBestiaryOverviewByKills ? EnumChatFormatting.AQUA : EnumChatFormatting.DARK_AQUA)
                .append(formattedKillsToGo).append(EnumChatFormatting.DARK_GRAY).append('/').append(formattedKillsGoal).append(EnumChatFormatting.GRAY).append(" kills");
        StringBuilder percentageInfo = new StringBuilder().append(sortBestiaryOverviewByKills ? EnumChatFormatting.DARK_AQUA : EnumChatFormatting.AQUA)
                .append(formattedPercentageToGo).append(EnumChatFormatting.DARK_GRAY).append("%");
        String spacer = EnumChatFormatting.DARK_GRAY + " ⬌ ";

        currentEntry.append(sortBestiaryOverviewByKills ? killsInfo : percentageInfo).append(spacer);
        int gapSize2 = ((sortBestiaryOverviewByKills
                ? (widestPercentageToGoWidth - fontRenderer.getStringWidth(formattedPercentageToGo) + widestKillGoalToGoWidth - fontRenderer.getStringWidth(formattedKillsGoal))
                : (widestKillsToGoWidth - fontRenderer.getStringWidth(formattedKillsToGo)))
                + remainingGap);
        int amountOf2ndSpacerChars = Math.max(gapSize2, 0) / spacerWidth;
        currentEntry.append(StringUtils.repeat(spacerChar, amountOf2ndSpacerChars));

        currentEntry.append(sortBestiaryOverviewByKills ? percentageInfo : killsInfo).append(EnumChatFormatting.GRAY).append(" to go");
        return currentEntry.toString();
    }

    public int getKillsToGo() {
        return killsToGo;
    }

    public int getPercentageToGo() {
        return percentageToGo;
    }

    public String getMobName() {
        return mobName;
    }
}
