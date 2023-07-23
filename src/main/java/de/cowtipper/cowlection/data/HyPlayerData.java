package de.cowtipper.cowlection.data;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;

import java.util.Map;

@SuppressWarnings("unused")
public class HyPlayerData {
    private String playername;
    private String displayname;
    private String rank;
    private String prefix;
    private String newPackageRank;
    private String rankPlusColor;
    private String monthlyPackageRank;
    private String monthlyRankColor;
    private long lastLogin;
    private long lastLogout;
    private String mostRecentGameType;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private Map<String, Integer> achievements;

    /**
     * No-args constructor for GSON
     */
    public HyPlayerData() {
    }

    public String getPlayerName() {
        return displayname;
    }

    public String getPlayerNameFormatted() {
        return getRankFormatted() + displayname;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public long getLastLogout() {
        return lastLogout;
    }

    public String getLastGame() {
        return DataHelper.GameType.getFancyName(mostRecentGameType);
    }

    public int getAchievement(String achievementName) {
        if (achievements != null) {
            return achievements.getOrDefault(achievementName, 0);
        }
        return 0;
    }

    public boolean hasNeverJoinedHypixel() {
        // example player that has never joined Hypixel (as of April 2020): Joe
        return rank == null && lastLogin == 0 && StringUtils.isNullOrEmpty(playername);
    }

    public boolean hasNeverLoggedOut() {
        // example player that has no logout value (as of October 2022): Creeper (in general accounts that haven't logged in for a few years)
        return lastLogin != 0 && lastLogout == 0;
    }

    public boolean isHidingOnlineStatus() {
        // example players: any higher ranked player (mods, admins, ...)
        return lastLogin == 0 && lastLogout == 0;
    }

    /**
     * Player's Rank prefix: <a href="https://github.com/HypixelDev/PublicAPI/wiki/Common-Questions#how-do-i-get-a-players-rank-prefix">API Docs: How do I get a player's rank prefix?</a>
     *
     * @return formatted rank
     */
    private String getRankFormatted() {
        if (prefix != null) {
            return prefix;
        }
        if (rank != null) {
            switch (rank) {
                case "HELPER":
                    return EnumChatFormatting.BLUE + "[HELPER] ";
                case "MODERATOR":
                    return EnumChatFormatting.DARK_GREEN + "[MOD] ";
                case "GAME_MASTER":
                    return EnumChatFormatting.DARK_GREEN + "[GM] ";
                case "ADMIN":
                    return EnumChatFormatting.RED + "[ADMIN] ";
                case "YOUTUBER":
                    return EnumChatFormatting.RED + "[" + EnumChatFormatting.WHITE + "YOUTUBE" + EnumChatFormatting.RED + "] ";
                default:
                    // unknown rank, fall-through
                    break;
            }
        }
        if (rankPlusColor == null) {
            rankPlusColor = "RED";
        }
        if (monthlyPackageRank != null && monthlyPackageRank.equals("SUPERSTAR")) {
            // MVP++
            EnumChatFormatting rankPlusPlusColor = monthlyRankColor != null ? EnumChatFormatting.getValueByName(monthlyRankColor) : EnumChatFormatting.GOLD;
            return rankPlusPlusColor + "[MVP" + EnumChatFormatting.getValueByName(rankPlusColor) + "++" + rankPlusPlusColor + "] ";
        }
        if (newPackageRank != null) {
            switch (newPackageRank) {
                case "VIP":
                    return EnumChatFormatting.GREEN + "[VIP] ";
                case "VIP_PLUS":
                    return EnumChatFormatting.GREEN + "[VIP" + EnumChatFormatting.GOLD + "+" + EnumChatFormatting.GREEN + "] ";
                case "MVP":
                    return EnumChatFormatting.AQUA + "[MVP] ";
                case "MVP_PLUS":
                    return EnumChatFormatting.AQUA + "[MVP" + EnumChatFormatting.getValueByName(rankPlusColor) + "+" + EnumChatFormatting.AQUA + "] ";
                default:
                    return EnumChatFormatting.GRAY.toString();
            }
        }
        return EnumChatFormatting.GRAY.toString();
    }
}
