package de.cowtipper.cowlection.handler;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.data.Friend;
import de.cowtipper.cowlection.util.GsonUtils;
import io.netty.util.internal.ConcurrentSet;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class FriendsHandler {
    private final Cowlection main;
    private final Set<Friend> bestFriends = new ConcurrentSet<>();
    private final File bestFriendsFile;

    public FriendsHandler(Cowlection main, File friendsFile) {
        this.main = main;
        this.bestFriendsFile = friendsFile;
        loadBestFriends();
    }

    public boolean removeBestFriend(String name) {
        boolean removed = bestFriends.removeIf(friend -> friend.getName().equalsIgnoreCase(name));
        if (removed) {
            if (bestFriends.isEmpty()) {
                MooConfig.doBestFriendsOnlineCheck = false;
                main.getConfig().syncFromFields();
            }
            saveBestFriends();
        }
        return removed;
    }

    public void removeAllBestFriends() {
        bestFriends.clear();
        MooConfig.doBestFriendsOnlineCheck = false;
        main.getConfig().syncFromFields();
        saveBestFriends();
    }

    public Set<String> getBestFriends() {
        return bestFriends.stream().map(Friend::getName).collect(Collectors.toCollection(TreeSet::new));
    }

    public int getBestFriendsListSize() {
        return bestFriends.size();
    }

    public synchronized void saveBestFriends() {
        try {
            String bestFriendsJsonZoned = GsonUtils.toJson(this.bestFriends);
            FileUtils.writeStringToFile(this.bestFriendsFile, bestFriendsJsonZoned, StandardCharsets.UTF_8);
        } catch (IOException e) {
            main.getLogger().error("Couldn't save best friends", e);
        }
    }

    private void loadBestFriends() {
        try {
            if (this.bestFriendsFile.exists()) {
                String bestFriendsData = FileUtils.readFileToString(this.bestFriendsFile, StandardCharsets.UTF_8);
                if (bestFriendsData.length() > 0) {
                    this.bestFriends.addAll(parseJson(bestFriendsData));
                }
            }
        } catch (IOException e) {
            main.getLogger().error("Couldn't read best friends file " + this.bestFriendsFile, e);
        } catch (JsonParseException e) {
            main.getLogger().error("Couldn't parse best friends file " + this.bestFriendsFile, e);
        }
    }

    private Set<Friend> parseJson(String bestFriendsData) {
        Type collectionType = new TypeToken<Set<Friend>>() {
        }.getType();
        return GsonUtils.fromJson(bestFriendsData, collectionType);
    }
}
