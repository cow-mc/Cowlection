package de.cowtipper.cowlection.handler;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.listener.skyblock.DungeonsPartyListener;
import de.cowtipper.cowlection.util.TickDelay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldSettings;

import java.util.*;
import java.util.stream.Collectors;

public class DungeonCache {
    private static final Ordering<NetworkPlayerInfo> playerOrderer = Ordering.from(new PlayerComparator());

    private final Cowlection main;
    private final Map<String, Integer> deathCounter;
    private final Set<String> deadPlayers;
    private final Set<String> failedPuzzles;
    private final Set<UUID> destroyedCrypts;
    private int destroyedCryptsInTabList;

    private boolean isInDungeon;
    private int elapsedMinutes;
    private int classMilestone;
    private long lastScoreboardCheck;
    private long nextPerformanceSend;
    private String queuedFloor;
    private List<String> potentialPartyMembers;

    public DungeonCache(Cowlection main) {
        this.main = main;
        deathCounter = new HashMap<>();
        deadPlayers = new HashSet<>();
        failedPuzzles = new HashSet<>();
        destroyedCrypts = new HashSet<>();
        destroyedCryptsInTabList = 0;
    }

    public boolean isInDungeon() {
        return isInDungeon;
    }

    public void onDungeonEnterOrLeave(boolean isInDungeonNow) {
        boolean wasInDungeon = isInDungeon;

        if (!wasInDungeon && isInDungeonNow) {
            onDungeonEntered();
        } else if (wasInDungeon && !isInDungeonNow) {
            onDungeonLeft();
        }
    }

    public void onDungeonEntered() {
        main.getLogger().info("Entered SkyBlock Dungeon!");
        isInDungeon = true;
        resetCounters();
    }

    public void onDungeonLeft() {
        main.getLogger().info("Leaving SkyBlock Dungeon!");
        isInDungeon = false;
        resetCounters();
    }

    public void sendDungeonPerformance() {
        if (System.currentTimeMillis() < nextPerformanceSend) {
            // already sent dungeon performance less than 260ms ago
            return;
        }
        String dungeonPerformance;
        boolean hasPointPenalty = false;
        if (deathCounter.isEmpty()) {
            dungeonPerformance = EnumChatFormatting.GOLD + "☠ Deaths: " + EnumChatFormatting.WHITE + "none \\o/";
        } else {
            hasPointPenalty = true;
            String deaths = deathCounter.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).map(deathEntry -> "  " + EnumChatFormatting.WHITE + deathEntry.getKey() + ": " + EnumChatFormatting.LIGHT_PURPLE + deathEntry.getValue())
                    .collect(Collectors.joining("\n"));
            dungeonPerformance = EnumChatFormatting.RED + "☠ " + EnumChatFormatting.BOLD + "Deaths: " + EnumChatFormatting.DARK_RED + "-" + (getTotalDeaths() * 2) + " Skill points\n" + deaths;
        }
        if (failedPuzzles.size() > 0) {
            hasPointPenalty = true;
            dungeonPerformance += "\n" + EnumChatFormatting.RED + "Failed puzzles: " + EnumChatFormatting.LIGHT_PURPLE + failedPuzzles.size() + EnumChatFormatting.RED + " (" + EnumChatFormatting.DARK_RED + "-" + failedPuzzles.size() * 14 + " Skill points" + EnumChatFormatting.RED + ")";
        }

        if (hasPointPenalty) {
            dungeonPerformance += "\n" + EnumChatFormatting.LIGHT_PURPLE + "➜ " + EnumChatFormatting.RED + EnumChatFormatting.BOLD + "Skill " + EnumChatFormatting.RED + "score penalty: " + EnumChatFormatting.DARK_RED + getSkillScorePenalty() + " points";
        }
        main.getChatHelper().sendMessage(EnumChatFormatting.WHITE, dungeonPerformance);
        nextPerformanceSend = System.currentTimeMillis() + 260;
    }

    /**
     * Fetch info from scoreboard (right) and tab list
     */
    public void fetchScoreboardData() {
        long now = System.currentTimeMillis();
        if (now - lastScoreboardCheck > 10000) { // run every 10 seconds
            lastScoreboardCheck = now;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld != null) {
                Scoreboard scoreboard = mc.theWorld.getScoreboard();

                // check scoreboard (right)
                ScoreObjective scoreboardSidebar = scoreboard.getObjectiveInDisplaySlot(1);
                if (scoreboardSidebar != null) {
                    Collection<Score> scoreboardLines = scoreboard.getSortedScores(scoreboardSidebar);
                    for (Score line : scoreboardLines) {
                        ScorePlayerTeam scorePlayerTeam = scoreboard.getPlayersTeam(line.getPlayerName());
                        if (scorePlayerTeam != null) {
                            String lineWithoutFormatting = EnumChatFormatting.getTextWithoutFormattingCodes(scorePlayerTeam.getColorPrefix() + scorePlayerTeam.getColorSuffix());

                            String timeElapsed = "Time Elapsed: ";
                            if (lineWithoutFormatting.startsWith(timeElapsed)) {
                                // dungeon timer: 05m 22s
                                String timeString = lineWithoutFormatting.substring(timeElapsed.length());
                                try {
                                    int indexOfMinute = timeString.indexOf('m');
                                    if (indexOfMinute > -1) {
                                        elapsedMinutes = (Integer.parseInt(timeString.substring(0, indexOfMinute)));
                                    }
                                } catch (NumberFormatException ex) {
                                    // couldn't parse dungeon time from scoreboard
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }

            // check tab list
            Collection<NetworkPlayerInfo> playerInfoMap = mc.thePlayer.sendQueue.getPlayerInfoMap();
            List<NetworkPlayerInfo> networkPlayerInfos = playerOrderer.sortedCopy(playerInfoMap);
            GuiPlayerTabOverlay tabList = mc.ingameGUI.getTabList();
            for (NetworkPlayerInfo playerInfo : networkPlayerInfos) {
                if (playerInfo.getGameProfile().getName().startsWith("!")) {
                    String tabListEntry = EnumChatFormatting.getTextWithoutFormattingCodes(tabList.getPlayerName(playerInfo));
                    if (tabListEntry != null && tabListEntry.startsWith(" Crypts: ")) {
                        try {
                            int cryptsCounterBegin = " Crypts: ".length();
                            int firstSlash = tabListEntry.indexOf('/'); // e.g. 4/16 -> only parse '4'
                            int cryptsCounterEnd = firstSlash > 0 ? firstSlash : tabListEntry.length();
                            destroyedCryptsInTabList = Integer.parseInt(tabListEntry.substring(cryptsCounterBegin, cryptsCounterEnd).trim());
                        } catch (NumberFormatException | IndexOutOfBoundsException ignored) {
                            // couldn't parse crypts count from tab list
                        }
                    }
                }
            }
        }
    }

    public void lookupPartyMembers() {
        if (potentialPartyMembers.size() > 0) {
            new DungeonsPartyListener(main, potentialPartyMembers);
            potentialPartyMembers.clear();
        }
    }

    // setter/adder
    public void setPotentialPartyMembers(List<String> potentialPartyMembers) {
        this.potentialPartyMembers = potentialPartyMembers;
    }

    public void setQueuedFloor(String floorNr) {
        this.queuedFloor = floorNr;
    }

    public void addDeath(String playerName) {
        boolean playerWasDeadAlready = !deadPlayers.add(playerName);
        if (playerWasDeadAlready) {
            // dead player "died" again (e.g. caused by disconnecting while being dead); don't count again!
            return;
        }
        int previousPlayerDeaths = deathCounter.getOrDefault(playerName, 0);
        deathCounter.put(playerName, previousPlayerDeaths + 1);

        if (MooConfig.dungSendPerformanceOnDeath) {
            new TickDelay(this::sendDungeonPerformance, 1);
        }
    }

    public void revivedPlayer(String playerName) {
        deadPlayers.remove(playerName);
    }

    public void addFailedPuzzle(String text) {
        failedPuzzles.add(text);
    }

    public void setClassMilestone(int classMilestone) {
        this.classMilestone = classMilestone;
    }

    public void addDestroyedCrypt(UUID uuid) {
        destroyedCrypts.add(uuid);
    }

    // getter
    public String getQueuedFloor() {
        return queuedFloor;
    }

    public int getMaxSkillScore() {
        return 100 - getSkillScorePenalty();
    }

    private int getSkillScorePenalty() {
        return getTotalDeaths() * 2 + failedPuzzles.size() * 14;
    }

    public int getTotalDeaths() {
        int totalDeaths = 0;
        for (int deathCount : deathCounter.values()) {
            totalDeaths += deathCount;
        }
        return totalDeaths;
    }

    public int getFailedPuzzles() {
        return failedPuzzles.size();
    }

    public int getClassMilestone() {
        return classMilestone;
    }

    public int getDestroyedCrypts() {
        return destroyedCryptsInTabList > 0 ? destroyedCryptsInTabList : destroyedCrypts.size();
    }

    public int getElapsedMinutes() {
        return elapsedMinutes;
    }

    // resetter
    private void resetCounters() {
        deathCounter.clear();
        deadPlayers.clear();
        failedPuzzles.clear();
        destroyedCrypts.clear();
        destroyedCryptsInTabList = 0;
        elapsedMinutes = 0;
        classMilestone = 0;
        nextPerformanceSend = 0;
        queuedFloor = null;
    }

    /**
     * see: GuiPlayerTabOverlay.PlayerComparator
     */
    static class PlayerComparator implements Comparator<NetworkPlayerInfo> {
        private PlayerComparator() {
        }

        public int compare(NetworkPlayerInfo playerInfo1, NetworkPlayerInfo playerInfo2) {
            ScorePlayerTeam scorePlayerTeam1 = playerInfo1.getPlayerTeam();
            ScorePlayerTeam scorePlayerTeam2 = playerInfo2.getPlayerTeam();
            return ComparisonChain.start().compareTrueFirst(playerInfo1.getGameType() != WorldSettings.GameType.SPECTATOR, playerInfo2.getGameType() != WorldSettings.GameType.SPECTATOR).compare(scorePlayerTeam1 != null ? scorePlayerTeam1.getRegisteredName() : "", scorePlayerTeam2 != null ? scorePlayerTeam2.getRegisteredName() : "").compare(playerInfo1.getGameProfile().getName(), playerInfo2.getGameProfile().getName()).result();
        }
    }
}
