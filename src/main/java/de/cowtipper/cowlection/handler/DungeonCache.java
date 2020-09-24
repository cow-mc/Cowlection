package de.cowtipper.cowlection.handler;

import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.util.TickDelay;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;

import java.util.*;
import java.util.stream.Collectors;

public class DungeonCache {
    private final Cowlection main;
    private final Map<String, Integer> deathCounter;
    private final Set<String> deadPlayers;
    private final Set<String> failedPuzzles;
    private final Set<UUID> destroyedCrypts;

    private boolean isInDungeon;
    private int elapsedMinutes;
    private int classMilestone;
    private long lastScoreboardCheck;
    private long nextPerformanceSend;
    private String queuedFloor;

    public DungeonCache(Cowlection main) {
        this.main = main;
        deathCounter = new HashMap<>();
        deadPlayers = new HashSet<>();
        failedPuzzles = new HashSet<>();
        destroyedCrypts = new HashSet<>();
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

    public void updateElapsedMinutesFromScoreboard() {
        long now = System.currentTimeMillis();
        if (now - lastScoreboardCheck > 10000) { // run every 10 seconds
            lastScoreboardCheck = now;
            Scoreboard scoreboard = Minecraft.getMinecraft().theWorld.getScoreboard();
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
    }

    // setter/adder
    public void setQueuedFloor(String floorNr) {
        this.queuedFloor = floorNr;
    }

    public void addDeath(String playerName, boolean ghostByDisconnecting) {
        if (!deadPlayers.add(playerName) && ghostByDisconnecting) {
            // dead player disconnected from the game; don't count again!
            return;
        }
        int previousPlayerDeaths = deathCounter.getOrDefault(playerName, 0);
        deathCounter.put(playerName, previousPlayerDeaths + 1);

        new TickDelay(this::sendDungeonPerformance, 1);
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

    public boolean addDestroyedCrypt(UUID uuid) {
        return destroyedCrypts.add(uuid);
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
        return destroyedCrypts.size();
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
        elapsedMinutes = 0;
        classMilestone = 0;
        nextPerformanceSend = 0;
        queuedFloor = null;
    }
}
