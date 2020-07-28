package de.cowtipper.cowlection.event;

import net.minecraftforge.fml.common.eventhandler.Event;

public class ApiErrorEvent extends Event {
    private final String playerName;

    public ApiErrorEvent(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }
}
