package eu.olli.cowmoonication.command;

import com.mojang.realmsclient.util.Pair;
import eu.olli.cowmoonication.Cowmoonication;
import eu.olli.cowmoonication.config.MooConfig;
import eu.olli.cowmoonication.config.MooGuiConfig;
import eu.olli.cowmoonication.data.Friend;
import eu.olli.cowmoonication.util.ApiUtils;
import eu.olli.cowmoonication.data.HyStalkingData;
import eu.olli.cowmoonication.util.TickDelay;
import eu.olli.cowmoonication.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.*;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MooCommand extends CommandBase {
    private final Cowmoonication main;

    public MooCommand(Cowmoonication main) {
        this.main = main;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sendCommandUsage(sender);
            return;
        }
        // sub commands: friends
        if (args[0].equalsIgnoreCase("stalk")) {
            if (args.length != 2) {
                main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Usage: /" + getCommandName() + " stalk <playerName>");
            } else if (!Utils.isValidMcName(args[1])) {
                main.getChatHelper().sendMessage(EnumChatFormatting.RED, "\"" + args[1] + "\" is not a valid player name.");
            } else {
                handleStalking(args[1]);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            handleBestFriendAdd(args[1]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            handleBestFriendRemove(args[1]);
        } else if (args[0].equalsIgnoreCase("list")) {
            handleListBestFriends();
        } else if (args[0].equalsIgnoreCase("nameChangeCheck")) {
            main.getChatHelper().sendMessage(EnumChatFormatting.GOLD, "Looking for best friends that have changed their name... This will take a few seconds...");
            main.getFriendsHandler().updateBestFriends(true);
        }
        // sub-commands: miscellaneous
        else if (args[0].equalsIgnoreCase("config") || args[0].equalsIgnoreCase("toggle")) {
            new TickDelay(() -> Minecraft.getMinecraft().displayGuiScreen(new MooGuiConfig(null)), 1); // delay by 1 tick, because the chat closing would close the new gui instantly as well.
        } else if (args[0].equalsIgnoreCase("guiscale")) {
            int currentGuiScale = (Minecraft.getMinecraft()).gameSettings.guiScale;
            if (args.length == 1) {
                main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "\u279C Current GUI scale: " + EnumChatFormatting.DARK_GREEN + currentGuiScale);
            } else {
                int scale = Math.min(10, MathHelper.parseIntWithDefault(args[1], 6));
                Minecraft.getMinecraft().gameSettings.guiScale = scale;
                main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "\u2714 New GUI scale: " + EnumChatFormatting.DARK_GREEN + scale + EnumChatFormatting.GREEN + " (previous: " + EnumChatFormatting.DARK_GREEN + currentGuiScale + EnumChatFormatting.GREEN + ")");
            }
        } else if (args[0].equalsIgnoreCase("shrug")) {
            main.getChatHelper().sendShrug(buildString(args, 1));
        } else if (args[0].equalsIgnoreCase("apikey")) {
            handleApiKey(args);
        }
        // sub-commands: update mod
        else if (args[0].equalsIgnoreCase("update")) {
            boolean updateCheckStarted = main.getVersionChecker().runUpdateCheck(true);

            if (updateCheckStarted) {
                main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "\u279C Checking for a newer mod version...");
                // VersionChecker#handleVersionStatus will run with a 5 seconds delay
            } else {
                long nextUpdate = main.getVersionChecker().getNextCheck();
                String waitingTime = String.format("%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(nextUpdate),
                        TimeUnit.MILLISECONDS.toSeconds(nextUpdate) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(nextUpdate)));
                main.getChatHelper().sendMessage(EnumChatFormatting.RED, "\u26A0 Update checker is on cooldown. Please wait " + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + waitingTime + EnumChatFormatting.RESET + EnumChatFormatting.RED + " more minutes before checking again.");
            }
        } else if (args[0].equalsIgnoreCase("updateHelp")) {
            main.getChatHelper().sendMessage(new ChatComponentText("\u279C Update instructions:").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(true))
                    .appendSibling(new ChatComponentText("\n\u278A" + EnumChatFormatting.YELLOW + " download latest mod version").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false)
                            .setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, main.getVersionChecker().getDownloadUrl()))
                            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Download the latest version of " + Cowmoonication.MODNAME + "\n\u279C Click to download latest mod file")))))
                    .appendSibling(new ChatComponentText("\n\u278B" + EnumChatFormatting.YELLOW + " exit Minecraft").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false)
                            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.GOLD + "\u278B" + EnumChatFormatting.YELLOW + " Without closing Minecraft first,\n" + EnumChatFormatting.YELLOW + "you can't delete the old .jar file!")))))
                    .appendSibling(new ChatComponentText("\n\u278C" + EnumChatFormatting.YELLOW + " copy " + EnumChatFormatting.GOLD + Cowmoonication.MODNAME.replace(" ", "") + "-" + main.getVersionChecker().getNewVersion() + ".jar" + EnumChatFormatting.YELLOW + " into mods folder").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false)
                            .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/moo folder"))
                            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Open mods folder with command " + EnumChatFormatting.GOLD + "/moo folder\n\u279C Click to open mods folder")))))
                    .appendSibling(new ChatComponentText("\n\u278D" + EnumChatFormatting.YELLOW + " delete old mod file " + EnumChatFormatting.GOLD + Cowmoonication.MODNAME.replace(" ", "") + "-" + Cowmoonication.VERSION + ".jar ").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false)))
                    .appendSibling(new ChatComponentText("\n\u278E" + EnumChatFormatting.YELLOW + " start Minecraft again").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false))));
        } else if (args[0].equalsIgnoreCase("version")) {
            main.getVersionChecker().handleVersionStatus(true);
        } else if (args[0].equalsIgnoreCase("folder")) {
            try {
                Desktop.getDesktop().open(main.getModsFolder());
            } catch (IOException e) {
                main.getChatHelper().sendMessage(EnumChatFormatting.RED, "\u2716 An error occurred trying to open the mod's folder. I guess you have to open it manually \u00af\\_(\u30c4)_/\u00af");
                e.printStackTrace();
            }
        }
        // "catch-all" remaining sub-commands
        else {
            sendCommandUsage(sender);
        }
    }

    private void handleApiKey(String[] args) {
        if (args.length == 1) {
            String firstSentence;
            EnumChatFormatting color;
            EnumChatFormatting colorSecondary;
            if (Utils.isValidUuid(MooConfig.moo)) {
                firstSentence = "You already set your Hypixel API key.";
                color = EnumChatFormatting.GREEN;
                colorSecondary = EnumChatFormatting.DARK_GREEN;
            } else {
                firstSentence = "You haven't set your Hypixel API key yet.";
                color = EnumChatFormatting.RED;
                colorSecondary = EnumChatFormatting.DARK_RED;
            }
            main.getChatHelper().sendMessage(color, firstSentence + " Use " + colorSecondary + "/api new" + color + " to request a new API key from Hypixel or use " + colorSecondary + "/" + this.getCommandName() + " apikey <key>" + color + " to manually set your existing API key.");
        } else {
            String key = args[1];
            if (Utils.isValidUuid(key)) {
                MooConfig.moo = key;
                main.getConfig().syncFromFields();
                main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "Updated API key!");
            } else {
                main.getChatHelper().sendMessage(EnumChatFormatting.RED, "That doesn't look like a valid API key...");
            }
        }
    }

    private void handleStalking(String playerName) {
        if (!Utils.isValidUuid(MooConfig.moo)) {
            main.getChatHelper().sendMessage(EnumChatFormatting.RED, "You haven't set your Hypixel API key yet. Use " + EnumChatFormatting.DARK_RED + "/api new" + EnumChatFormatting.RED + " to request a new API key from Hypixel or use " + EnumChatFormatting.DARK_RED + "/" + this.getCommandName() + " apikey <key>" + EnumChatFormatting.RED + " to manually set your existing API key.");
            return;
        }
        main.getChatHelper().sendMessage(EnumChatFormatting.GRAY, "Stalking " + EnumChatFormatting.WHITE + playerName + EnumChatFormatting.GRAY + ". This may take a few seconds.");
        boolean isBestFriend = main.getFriendsHandler().isBestFriend(playerName, true);
        if (isBestFriend) {
            Friend stalkedPlayer = main.getFriendsHandler().getBestFriend(playerName);
            // we have the uuid already, so stalk the player
            stalkPlayer(stalkedPlayer);
        } else {
            // fetch player uuid
            ApiUtils.fetchFriendData(playerName, stalkedPlayer -> {
                if (stalkedPlayer == null) {
                    main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Sorry, could contact Mojang's API and thus couldn't stalk " + EnumChatFormatting.DARK_RED + playerName);
                } else if (stalkedPlayer.equals(Friend.FRIEND_NOT_FOUND)) {
                    main.getChatHelper().sendMessage(EnumChatFormatting.RED, "There is no player with the name " + EnumChatFormatting.DARK_RED + playerName + EnumChatFormatting.RED + ".");
                } else {
                    // ... then stalk the player
                    stalkPlayer(stalkedPlayer);
                }
            });
        }
    }

    private void stalkPlayer(Friend stalkedPlayer) {
        ApiUtils.fetchPlayerStatus(stalkedPlayer, hyStalking -> {
            if (hyStalking != null && hyStalking.isSuccess()) {
                HyStalkingData.HySession session = hyStalking.getSession();
                if (session.isOnline()) {
                    main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD + stalkedPlayer.getName() + EnumChatFormatting.YELLOW + " is currently playing " + EnumChatFormatting.GOLD + session.getGameType() + EnumChatFormatting.YELLOW
                            + (session.getMode() != null ? ": " + EnumChatFormatting.GOLD + session.getMode() : "")
                            + (session.getMap() != null ? EnumChatFormatting.YELLOW + " (Map: " + EnumChatFormatting.GOLD + session.getMap() + EnumChatFormatting.YELLOW + ")" : ""));
                } else {
                    ApiUtils.fetchPlayerOfflineStatus(stalkedPlayer, slothStalking -> {
                        if (slothStalking == null) {
                            main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Something went wrong contacting the Slothpixel API. Couldn't stalk " + EnumChatFormatting.DARK_RED + stalkedPlayer.getName() + EnumChatFormatting.RED + " but they appear to be offline currently.");
                        } else if (slothStalking.hasNeverJoinedHypixel()) {
                            main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD + stalkedPlayer.getName() + EnumChatFormatting.YELLOW + " has " + EnumChatFormatting.GOLD + "never " + EnumChatFormatting.YELLOW + "been on Hypixel (or might be nicked).");
                        } else if (slothStalking.isHidingOnlineStatus()) {
                            main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, slothStalking.getPlayerNameFormatted() + EnumChatFormatting.YELLOW + " is hiding their online status.");
                        } else if (slothStalking.hasNeverLoggedOut()) {
                            Pair<String, String> lastOnline = Utils.getLastOnlineWords(slothStalking.getLastLogin());

                            main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, slothStalking.getPlayerNameFormatted() + EnumChatFormatting.YELLOW + " was last online " + EnumChatFormatting.GOLD + lastOnline.first() + EnumChatFormatting.YELLOW + " ago"
                                    + (lastOnline.second() != null ? " (" + EnumChatFormatting.GOLD + lastOnline.second() + EnumChatFormatting.YELLOW + ")" : "") + ".");
                        } else {
                            Pair<String, String> lastOnline = Utils.getLastOnlineWords(slothStalking.getLastLogout());

                            main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, slothStalking.getPlayerNameFormatted() + EnumChatFormatting.YELLOW + " is " + EnumChatFormatting.GOLD + "offline" + EnumChatFormatting.YELLOW + " for " + EnumChatFormatting.GOLD + lastOnline.first() + EnumChatFormatting.YELLOW
                                    + ((lastOnline.second() != null || slothStalking.getLastGame() != null) ? (" ("
                                    + (lastOnline.second() != null ? EnumChatFormatting.GOLD + lastOnline.second() + EnumChatFormatting.YELLOW : "") // = last online date
                                    + (lastOnline.second() != null && slothStalking.getLastGame() != null ? "; " : "") // = delimiter
                                    + (slothStalking.getLastGame() != null ? "last played gamemode: " + EnumChatFormatting.GOLD + slothStalking.getLastGame() + EnumChatFormatting.YELLOW : "") // = last gamemode
                                    + ")") : "") + ".");
                        }
                    });
                }
            } else {
                String cause = (hyStalking != null) ? hyStalking.getCause() : null;
                main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Something went wrong contacting the Hypixel API. Couldn't stalk " + EnumChatFormatting.DARK_RED + stalkedPlayer.getName() + EnumChatFormatting.RED + (cause != null ? " (Reason: " + EnumChatFormatting.DARK_RED + cause + EnumChatFormatting.RED + ")" : "") + ".");
            }
        });
    }

    private void handleBestFriendAdd(String username) {
        if (!Utils.isValidMcName(username)) {
            main.getChatHelper().sendMessage(EnumChatFormatting.RED, EnumChatFormatting.DARK_RED + username + EnumChatFormatting.RED + "? This... doesn't look like a valid username.");
            return;
        }

        // TODO Add check if 'best friend' is on normal friend list
        if (main.getFriendsHandler().isBestFriend(username, true)) {
            main.getChatHelper().sendMessage(EnumChatFormatting.RED, EnumChatFormatting.DARK_RED + username + EnumChatFormatting.RED + " is a best friend already.");
        } else {
            main.getChatHelper().sendMessage(EnumChatFormatting.GOLD, "Fetching " + EnumChatFormatting.YELLOW + username + EnumChatFormatting.GOLD + "'s unique user id. This may take a few seconds...");
            // add friend async
            main.getFriendsHandler().addBestFriend(username);
        }
    }

    private void handleBestFriendRemove(String username) {
        if (!Utils.isValidMcName(username)) {
            main.getChatHelper().sendMessage(EnumChatFormatting.RED, EnumChatFormatting.DARK_RED + username + EnumChatFormatting.RED + "? This... doesn't look like a valid username.");
            return;
        }

        boolean removed = main.getFriendsHandler().removeBestFriend(username);
        if (removed) {
            main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "Removed " + EnumChatFormatting.DARK_GREEN + username + EnumChatFormatting.GREEN + " from best friends list.");
        } else {
            main.getChatHelper().sendMessage(EnumChatFormatting.RED, EnumChatFormatting.DARK_RED + username + EnumChatFormatting.RED + " isn't a best friend.");
        }
    }

    private void handleListBestFriends() {
        Set<String> bestFriends = main.getFriendsHandler().getBestFriends();

        // TODO show fancy gui with list of best friends; maybe with buttons to delete them
        main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "\u279C Best friends: " + EnumChatFormatting.DARK_GREEN + String.join(EnumChatFormatting.GREEN + ", " + EnumChatFormatting.DARK_GREEN, bestFriends));
    }

    @Override
    public String getCommandName() {
        return "moo";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + getCommandName() + " help";
    }

    private void sendCommandUsage(ICommandSender sender) {
        IChatComponent usage = new ChatComponentText("\u279C " + Cowmoonication.MODNAME + " commands:").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(true))
                .appendSibling(createCmdHelpSection(1, "Friends"))
                .appendSibling(createCmdHelpEntry("stalk", "Get info of player's status"))
                .appendSibling(createCmdHelpEntry("add", "Add best friends"))
                .appendSibling(createCmdHelpEntry("remove", "Remove best friends"))
                .appendSibling(createCmdHelpEntry("list", "View list of best friends"))
                .appendSibling(createCmdHelpEntry("nameChangeCheck", "Force a scan for changed names of best friends"))
                .appendSibling(createCmdHelpEntry("toggle", "Toggle join/leave notifications"))
                .appendSibling(createCmdHelpSection(2, "Miscellaneous"))
                .appendSibling(createCmdHelpEntry("config", "Open mod's configuration"))
                .appendSibling(createCmdHelpEntry("guiScale", "Change GUI scale"))
                .appendSibling(createCmdHelpEntry("shrug", "\u00AF\\_(\u30C4)_/\u00AF")) // ¯\_(ツ)_/¯
                .appendSibling(createCmdHelpSection(3, "Update mod"))
                .appendSibling(createCmdHelpEntry("update", "Check for new mod updates"))
                .appendSibling(createCmdHelpEntry("updateHelp", "Show mod update instructions"))
                .appendSibling(createCmdHelpEntry("version", "View results of last mod update check"))
                .appendSibling(createCmdHelpEntry("folder", "Open Minecraft's mods folder"));
        sender.addChatMessage(usage);
    }

    private IChatComponent createCmdHelpSection(int nr, String title) {
        String prefix = Character.toString((char) (0x2789 + nr));
        return new ChatComponentText("\n").appendSibling(new ChatComponentText(prefix + " " + title).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(true)));
    }

    private IChatComponent createCmdHelpEntry(String cmd, String usage) {
        String command = "/" + this.getCommandName() + " " + cmd;
        ChatStyle clickableMsg = new ChatStyle()
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Run " + EnumChatFormatting.GOLD + command)))
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
        return new ChatComponentText("\n").appendSibling(new ChatComponentText(command).setChatStyle(clickableMsg.createDeepCopy().setColor(EnumChatFormatting.GOLD)))
                .appendSibling(new ChatComponentText(" \u27A1 " + usage).setChatStyle(clickableMsg.createDeepCopy().setColor(EnumChatFormatting.YELLOW)));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args,
                    /* friends */  "stalk", "add", "remove", "list", "nameChangeCheck", "toggle",
                    /* miscellaneous */ "config", "guiscale", "shrug", "apikey",
                    /* update mod */ "update", "updateHelp", "version", "folder",
                    /* help */ "help");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return getListOfStringsMatchingLastWord(args, main.getFriendsHandler().getBestFriends());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("stalk")) {
            return getListOfStringsMatchingLastWord(args, main.getPlayerCache().getAllNamesSorted());
        }
        return null;
    }
}
