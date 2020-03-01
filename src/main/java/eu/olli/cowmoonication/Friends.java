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

    public boolean addBestFriend(String name, boolean save) {
        if (name.isEmpty()) {
            return false;
        }
        boolean added = bestFriends.add(name);
        if (added && save) {
            saveBestFriends();
        }
        return added;
    }

    public boolean addBestFriend(String name) {
        return addBestFriend(name, false);
    }

    public boolean removeBestFriend(String name) {
        boolean removed = bestFriends.remove(name);
        if (removed) {
            saveBestFriends();
        }
        return removed;
    }

    public boolean isBestFriend(String playerName) {
        return bestFriends.contains(playerName);
    }

    private void saveBestFriends() {

    }

    public Set<String> getBestFriends() {
        return new TreeSet<>(bestFriends);
    }

    public void syncFriends(String[] bestFriends) {
        this.bestFriends = new HashSet<>();
        Collections.addAll(this.bestFriends, bestFriends);
    }
}
