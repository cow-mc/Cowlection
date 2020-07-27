package de.cowtipper.cowlection.handler;

import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.util.TickDelay;
import net.minecraft.util.EnumChatFormatting;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DungeonCache {
    private boolean isInDungeon;
    private final Map<String, Integer> deathCounter;
    private final Cowlection main;

    public DungeonCache(Cowlection main) {
        this.main = main;
        deathCounter = new HashMap<>();
    }

    public void onDungeonEntered() {
        isInDungeon = true;
        deathCounter.clear();
    }

    public void onDungeonLeft() {
        isInDungeon = false;
        deathCounter.clear();
    }

    public void addDeath(String playerName) {
        int previousPlayerDeaths = deathCounter.getOrDefault(playerName, 0);
        deathCounter.put(playerName, previousPlayerDeaths + 1);

        new TickDelay(this::sendDeathCounts, 1);
    }

    public boolean isInDungeon() {
        return isInDungeon;
    }

    public void sendDeathCounts() {
        if (deathCounter.isEmpty()) {
            main.getChatHelper().sendMessage(EnumChatFormatting.GOLD, "☠ Deaths: " + EnumChatFormatting.WHITE + "none \\o/");
        } else {
            String deaths = deathCounter.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).map(deathEntry -> "  " + EnumChatFormatting.WHITE + deathEntry.getKey() + ": " + EnumChatFormatting.LIGHT_PURPLE + deathEntry.getValue())
                    .collect(Collectors.joining("\n"));
            main.getChatHelper().sendMessage(EnumChatFormatting.RED, "☠ " + EnumChatFormatting.BOLD + "Deaths:\n" + deaths);
        }
    }
}
