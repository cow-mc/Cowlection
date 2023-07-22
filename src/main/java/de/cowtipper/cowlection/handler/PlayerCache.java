package de.cowtipper.cowlection.handler;

import com.google.common.collect.EvictingQueue;

import java.util.SortedSet;
import java.util.TreeSet;

@SuppressWarnings("UnstableApiUsage")
public class PlayerCache {
    private final EvictingQueue<String> nameCache = EvictingQueue.create(50);
    private final EvictingQueue<String> friendCache = EvictingQueue.create(100);

    public PlayerCache() {
    }

    public void add(String name) {
        // remove old entry (if exists) to 'push' name to the end of the queue
        nameCache.remove(name);
        nameCache.add(name);
    }

    public void addFriend(String name) {
        // remove old entry (if exists) to 'push' name to the end of the queue
        friendCache.remove(name);
        friendCache.add(name);
    }

    public void removeFriend(String name) {
        friendCache.remove(name);
    }

    public SortedSet<String> getAllNamesSorted() {
        SortedSet<String> nameList = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        nameList.addAll(friendCache);
        nameList.addAll(nameCache);
        return nameList;
    }

    public void clearAllCaches() {
        nameCache.clear();
        friendCache.clear();
    }
}
