package eu.olli.cowmoonication;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class Friends {
    private final Cowmoonication main;
    private Set<String> bestFriends = new HashSet<>();

    public Friends(Cowmoonication main) {
        this.main = main;
    }

    public boolean isBestFriend(String playerName) {
        return bestFriends.contains(playerName);
    }

    public Set<String> getBestFriends() {
        return new TreeSet<>(bestFriends);
    }

    public void syncFriends(String[] bestFriends) {
        this.bestFriends = new HashSet<>();
        Collections.addAll(this.bestFriends, bestFriends);
    }
}
