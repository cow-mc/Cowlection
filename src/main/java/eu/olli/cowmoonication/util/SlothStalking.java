package eu.olli.cowmoonication.util;

public class SlothStalking {
    private String username;
    private String rank;
    private String rank_formatted;
    // private boolean online;
    private long last_login;
    private long last_logout;
    private String last_game;

    /**
     * No-args constructor for GSON
     */
    public SlothStalking() {
    }

    public String getPlayerNameFormatted() {
        return rank_formatted.replace('&', '§') + " " + username;
    }

    public long getLastLogin() {
        return last_login;
    }

    public long getLastLogout() {
        return last_logout;
    }

    public String getLastGame() {
        return last_game;
    }

    public boolean hasNeverJoinedHypixel() {
        // example player that has never joined Hypixel (as of April 2020): Joe
        return rank == null && last_login == 0;
    }

    public boolean hasNeverLoggedOut() {
        // example player that has no logout value (as of April 2020): Pig (in general accounts that haven't logged in for a few years)
        return last_login != 0 && last_logout == 0;
    }

    public boolean isHidingOnlineStatus() {
        // example players: any higher ranked player (mods, admins, ...)
        return last_login == 0 && last_logout == 0;
    }
}
