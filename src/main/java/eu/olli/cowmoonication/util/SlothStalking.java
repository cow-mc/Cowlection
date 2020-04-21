package eu.olli.cowmoonication.util;

public class SlothStalking {
    private String username;
    private String rank_formatted;
    // private boolean online;
    // private long last_login;
    private long last_logout;
    private String last_game;

    public SlothStalking() {
    }

    public String getPlayerNameFormatted() {
        return rank_formatted.replace('&', '§') + " " + username;
    }

    public long getLastLogout() {
        return last_logout;
    }

    public String getLastGame() {
        return last_game;
    }
}
