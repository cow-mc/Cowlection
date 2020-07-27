package de.cowtipper.cowlection.handler;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.command.exception.ApiContactException;
import de.cowtipper.cowlection.command.exception.MooCommandException;
import de.cowtipper.cowlection.data.Friend;
import de.cowtipper.cowlection.util.ApiUtils;
import de.cowtipper.cowlection.util.GsonUtils;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class FriendsHandler {
    private static final long UPDATE_FREQUENCY_DEFAULT = TimeUnit.HOURS.toMillis(10);
    private final Cowlection main;
    private final Set<Friend> bestFriends = new ConcurrentSet<>();
    private final File bestFriendsFile;
    private final AtomicInteger bestFriendQueue = new AtomicInteger();

    public FriendsHandler(Cowlection main, File friendsFile) {
        this.main = main;
        this.bestFriendsFile = friendsFile;
        loadBestFriends();
        updateBestFriends();
    }

    public boolean isBestFriend(String playerName, boolean ignoreCase) {
        if (ignoreCase) {
            return bestFriends.stream().map(Friend::getName).anyMatch(playerName::equalsIgnoreCase);
        } else {
            return bestFriends.stream().map(Friend::getName).anyMatch(playerName::equals);
        }
    }

    public void addBestFriend(String name) {
        if (name.isEmpty()) {
            return;
        }

        ApiUtils.fetchFriendData(name, friend -> {
            if (friend == null) {
                throw new ApiContactException("Mojang", "didn't add " + name + " as a best friend.");
            } else if (friend.equals(Friend.FRIEND_NOT_FOUND)) {
                throw new PlayerNotFoundException("There is no player with the name " + EnumChatFormatting.DARK_RED + name + EnumChatFormatting.RED + ".");
            } else {
                boolean added = bestFriends.add(friend);
                if (added) {
                    main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "Added " + EnumChatFormatting.DARK_GREEN + friend.getName() + EnumChatFormatting.GREEN + " as best friend.");
                    saveBestFriends();
                }
            }
        });
    }

    public boolean removeBestFriend(String name) {
        boolean removed = bestFriends.removeIf(friend -> friend.getName().equalsIgnoreCase(name));
        if (removed) {
            saveBestFriends();
        }
        return removed;
    }

    public Set<String> getBestFriends() {
        return bestFriends.stream().map(Friend::getName).collect(Collectors.toCollection(TreeSet::new));
    }

    public Friend getBestFriend(String name) {
        return bestFriends.stream().filter(friend -> friend.getName().equalsIgnoreCase(name)).findFirst().orElse(Friend.FRIEND_NOT_FOUND);
    }

    private Friend getBestFriend(UUID uuid) {
        return bestFriends.stream().filter(friend -> friend.getUuid().equals(uuid)).findFirst().orElse(Friend.FRIEND_NOT_FOUND);
    }

    public void updateBestFriends() {
        bestFriends.stream().filter(friend -> System.currentTimeMillis() - friend.getLastChecked() > UPDATE_FREQUENCY_DEFAULT)
                .forEach(friend1 -> {
                    bestFriendQueue.incrementAndGet();
                    updateBestFriend(friend1, false);
                });
    }

    public void updateBestFriend(Friend friend, boolean isCommandTriggered) {
        ApiUtils.fetchCurrentName(friend, newName -> {
            if (newName == null) {
                // skipping friend, something went wrong with API request
                if (isCommandTriggered) {
                    throw new ApiContactException("Mojang", "couldn't check " + EnumChatFormatting.DARK_RED + friend.getName() + EnumChatFormatting.RED + " (possible) new player name");
                }
            } else if (newName.equals(ApiUtils.UUID_NOT_FOUND)) {
                throw new PlayerNotFoundException("How did you manage to get a unique id on your best friends list that has no name attached to it?");
            } else if (newName.equals(friend.getName())) {
                // name hasn't changed, only updating lastChecked timestamp
                Friend bestFriend = getBestFriend(friend.getUuid());
                if (!bestFriend.equals(Friend.FRIEND_NOT_FOUND)) {
                    bestFriend.setLastChecked(System.currentTimeMillis());
                    if (isCommandTriggered) {
                        throw new MooCommandException(friend.getName() + " hasn't changed his name");
                    }
                }
            } else {
                // name has changed
                main.getChatHelper().sendMessage(new ChatComponentText("Your best friend " + EnumChatFormatting.DARK_GREEN + friend.getName() + EnumChatFormatting.GREEN + " changed the name to " + EnumChatFormatting.DARK_GREEN + newName + EnumChatFormatting.GREEN + ".").setChatStyle(new ChatStyle()
                        .setColor(EnumChatFormatting.GREEN)
                        .setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://namemc.com/search?q=" + newName))
                        .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "View " + EnumChatFormatting.GOLD + newName + EnumChatFormatting.YELLOW + "'s name history on namemc.com")))));

                Friend bestFriend = getBestFriend(friend.getUuid());
                if (!bestFriend.equals(Friend.FRIEND_NOT_FOUND)) {
                    bestFriend.setName(newName);
                    bestFriend.setLastChecked(System.currentTimeMillis());
                }
            }
            if (isCommandTriggered) {
                saveBestFriends();
            } else {
                int remainingFriendsToCheck = bestFriendQueue.decrementAndGet();
                if (remainingFriendsToCheck == 0) {
                    // we're done with checking for name changes, save updates to file!
                    saveBestFriends();
                }
            }
        });
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
            boolean createdNewFile = this.bestFriendsFile.createNewFile();

            this.bestFriends.clear();
            if (!createdNewFile) {
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
