package eu.olli.cowlection.handler;

import com.google.common.collect.EvictingQueue;
import eu.olli.cowlection.Cowlection;

import java.util.SortedSet;
import java.util.TreeSet;

public class PlayerCache {
    @SuppressWarnings("UnstableApiUsage")
    private final EvictingQueue<String> nameCache = EvictingQueue.create(50);
    @SuppressWarnings("UnstableApiUsage")
    private final EvictingQueue<String> bestFriendCache = EvictingQueue.create(50);
    private final Cowlection main;

    public PlayerCache(Cowlection main) {
        this.main = main;
    }

    public void add(String name) {
        // remove old entry (if exists) to 'push' name to the end of the queue
        nameCache.remove(name);
        nameCache.add(name);
    }

    public void addBestFriend(String name) {
        // remove old entry (if exists) to 'push' name to the end of the queue
        bestFriendCache.remove(name);
        bestFriendCache.add(name);
    }

    public void removeBestFriend(String name) {
        bestFriendCache.remove(name);
    }

    public SortedSet<String> getAllNamesSorted() {
        SortedSet<String> nameList = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        nameList.addAll(bestFriendCache);
        nameList.addAll(nameCache);
        return nameList;
    }

    public void clearAllCaches() {
        nameCache.clear();
        bestFriendCache.clear();
    }
}
