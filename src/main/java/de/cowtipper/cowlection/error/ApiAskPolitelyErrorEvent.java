package de.cowtipper.cowlection.error;

import net.minecraftforge.fml.common.eventhandler.Event;

public class ApiAskPolitelyErrorEvent extends Event {
    private final String playerName;

    public ApiAskPolitelyErrorEvent(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }
}
