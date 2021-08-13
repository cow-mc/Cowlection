package de.cowtipper.cowlection.command;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.realmsclient.util.Pair;
import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.chesttracker.ChestOverviewGui;
import de.cowtipper.cowlection.command.exception.ApiContactException;
import de.cowtipper.cowlection.command.exception.InvalidPlayerNameException;
import de.cowtipper.cowlection.command.exception.MooCommandException;
import de.cowtipper.cowlection.config.CredentialStorage;
import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.config.gui.MooConfigGui;
import de.cowtipper.cowlection.data.*;
import de.cowtipper.cowlection.data.HySkyBlockStats.Profile.Pet;
import de.cowtipper.cowlection.handler.DungeonCache;
import de.cowtipper.cowlection.listener.skyblock.DungeonsPartyListener;
import de.cowtipper.cowlection.partyfinder.RuleEditorGui;
import de.cowtipper.cowlection.search.GuiSearch;
import de.cowtipper.cowlection.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.command.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.*;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.common.util.Constants;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MooCommand extends CommandBase {
    private final Cowlection main;
    private DungeonsPartyListener dungeonsPartyListener;

    public MooCommand(Cowlection main) {
        this.main = main;
    }

    @Override
    public String getCommandName() {
        return "moo";
    }

    @Override
    public List<String> getCommandAliases() {
        List<String> aliases = new ArrayList<>();
        if (StringUtils.isNotEmpty(MooConfig.mooCmdAlias)) {
            aliases.add(MooConfig.mooCmdAlias);
        }
        return aliases;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + getCommandName() + " help";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            main.getChatHelper().sendMessage(EnumChatFormatting.GOLD, "Tried to say " + EnumChatFormatting.YELLOW + getCommandName() + EnumChatFormatting.GOLD + "? Use " + EnumChatFormatting.YELLOW + getCommandName() + " say [optional text]" + EnumChatFormatting.GOLD + " instead.\n"
                    + "Tried to use the command " + EnumChatFormatting.YELLOW + "/" + getCommandName() + EnumChatFormatting.GOLD + "? Use " + EnumChatFormatting.YELLOW + "/" + getCommandName() + " help" + EnumChatFormatting.GOLD + " for a list of available commands");
        }
        //region sub commands: Best friends, friends & other players
        else if (args[0].equalsIgnoreCase("say")) {
            // work-around so you can still say 'moo' in chat without triggering the client-side command
            String msg = CommandBase.buildString(args, 1);
            Minecraft.getMinecraft().thePlayer.sendChatMessage(getCommandName() + (!msg.isEmpty() ? " " + msg : ""));
        } else if (args[0].equalsIgnoreCase("stalk")
                || args[0].equalsIgnoreCase("s")
                || args[0].equalsIgnoreCase("askPolitelyWhereTheyAre")) {
            handleStalking(args);
        } else if (args[0].equalsIgnoreCase("add")) {
            handleBestFriendAdd(args);
        } else if (args[0].equalsIgnoreCase("remove")) {
            handleBestFriendRemove(args);
        } else if (args[0].equalsIgnoreCase("list")) {
            handleListBestFriends();
        } else if (args[0].equalsIgnoreCase("online")) {
            handleBestFriendsOnlineCheck();
        } else if (args[0].equalsIgnoreCase("nameChangeCheck")) {
            handleNameChangeCheck(args);
        }
        // + toggle (= alias for config)
        //endregion
        //region sub commands: SkyBlock
        else if (args[0].equalsIgnoreCase("stalkskyblock") || args[0].equalsIgnoreCase("skyblockstalk")
                || args[0].equalsIgnoreCase("ss")
                || args[0].equalsIgnoreCase("stalksb") || args[0].equalsIgnoreCase("sbstalk")
                || args[0].equalsIgnoreCase("askPolitelyAboutTheirSkyBlockProgress")) {
            handleStalkingSkyBlock(args);
        } else if (args[0].equalsIgnoreCase("chestAnalyzer") || args[0].equalsIgnoreCase("chestAnalyser") || args[0].equalsIgnoreCase("analyzeChests") || args[0].equalsIgnoreCase("analyseChests")) {
            handleAnalyzeChests(args);
        } else if (args[0].equalsIgnoreCase("analyzeIsland") || args[0].equalsIgnoreCase("analyseIsland")) {
            handleAnalyzeIsland(sender);
        } else if (args[0].equalsIgnoreCase("waila") || args[0].equalsIgnoreCase("whatAmILookingAt")) {
            boolean showAllInfo = MooConfig.keepFullWailaInfo();
            if (args.length == 2) {
                if (args[1].equalsIgnoreCase("all")) {
                    showAllInfo = true;
                } else if (args[1].equalsIgnoreCase("main")) {
                    showAllInfo = false;
                }
            }
            handleWhatAmILookingAt(sender, showAllInfo);
        } else if (args[0].equalsIgnoreCase("dungeon") || args[0].equalsIgnoreCase("dung")
                || /* dungeon party: */ args[0].equalsIgnoreCase("dp")
                || /* dungeon party finder rules: */ args[0].equalsIgnoreCase("dr")) {
            handleDungeon(args);
        }
        //endregion
        //region sub-commands: miscellaneous
        else if (args[0].equalsIgnoreCase("config")) {
            main.getConfig().theyOpenedTheConfigGui();
            displayGuiScreen(new MooConfigGui(buildString(args, 1)));
        } else if (args[0].equalsIgnoreCase("search")) {
            displayGuiScreen(new GuiSearch(CommandBase.buildString(args, 1)));
        } else if (args[0].equalsIgnoreCase("guiscale")) {
            handleGuiScale(args);
        } else if (args[0].equalsIgnoreCase("rr")) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage("/r " + CommandBase.buildString(args, 1));
        } else if (args[0].equalsIgnoreCase("shrug")) {
            main.getChatHelper().sendShrug(buildString(args, 1));
        } else if (args[0].equalsIgnoreCase("apikey")) {
            handleApiKey(args);
        } else if (args[0].equalsIgnoreCase("whatyearisit") || args[0].equalsIgnoreCase("year")) {
            long year = ((System.currentTimeMillis() - 1560275700000L) / (TimeUnit.HOURS.toMillis(124))) + 1;
            main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, "It is SkyBlock year " + EnumChatFormatting.GOLD + year + EnumChatFormatting.YELLOW + ".");
        } else if (args[0].equalsIgnoreCase("worldage") || args[0].equalsIgnoreCase("serverage")) {
            handleWorldAge(args);
        } else if (args[0].equalsIgnoreCase("discord")) {
            main.getChatHelper().sendMessage(new MooChatComponent("➜ Need help with " + EnumChatFormatting.GOLD + Cowlection.MODNAME + EnumChatFormatting.GREEN + "? Do you have any questions, suggestions or other feedback? " + EnumChatFormatting.GOLD + "Join the Cowshed discord!").green().setUrl(Cowlection.INVITE_URL));
        }
        //endregion
        //region sub-commands: update mod
        else if (args[0].equalsIgnoreCase("update")) {
            handleUpdate(args);
        } else if (args[0].equalsIgnoreCase("updateHelp")) {
            handleUpdateHelp();
        } else if (args[0].equalsIgnoreCase("version")) {
            main.getVersionChecker().handleVersionStatus(true);
        } else if (args[0].equalsIgnoreCase("directory") || args[0].equalsIgnoreCase("folder")) {
            try {
                Desktop.getDesktop().open(main.getModsDirectory());
            } catch (IOException e) {
                e.printStackTrace();
                throw new MooCommandException("✖ An error occurred trying to open the mod's directory. I guess you have to open it manually ¯\\_(ツ)_/¯");
            }
        }
        //endregion
        // help
        else if (args[0].equalsIgnoreCase("help")) {
            sendCommandUsage(sender);
        }
        // fix: run server-side command /m with optional arguments
        else if (args[0].equalsIgnoreCase("cmd") || args[0].equalsIgnoreCase("command")) {
            String cmdArgs = CommandBase.buildString(args, 1);
            if (cmdArgs.length() > 0) {
                cmdArgs = " " + cmdArgs;
            }
            Minecraft.getMinecraft().thePlayer.sendChatMessage("/" + MooConfig.mooCmdAlias + cmdArgs);
        }
        // "catch-all" remaining sub-commands
        else {
            main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Command " + EnumChatFormatting.DARK_RED + "/" + getCommandName() + " " + args[0] + EnumChatFormatting.RED + " doesn't exist. Use " + EnumChatFormatting.DARK_RED + "/" + getCommandName() + " help " + EnumChatFormatting.RED + "to show command usage."
                    + (StringUtils.isNotEmpty(MooConfig.mooCmdAlias) ? "\n" + EnumChatFormatting.RED + "Are you trying to use a server-side command " + EnumChatFormatting.DARK_RED + "/" + MooConfig.mooCmdAlias + EnumChatFormatting.RED + "? Use " + EnumChatFormatting.DARK_RED + "/" + MooConfig.mooCmdAlias + " cmd [arguments] " + EnumChatFormatting.RED + "instead." : ""));
        }
    }

    //region sub commands: Best friends, friends & other players
    private void handleStalking(String[] args) throws CommandException {
        if (!CredentialStorage.isMooValid) {
            throw new MooCommandException("You haven't set your Hypixel API key yet or the API key is invalid. Use " + EnumChatFormatting.DARK_RED + "/api new" + EnumChatFormatting.RED + " to request a new API key from Hypixel or use " + EnumChatFormatting.DARK_RED + "/" + this.getCommandName() + " apikey <key>" + EnumChatFormatting.RED + " to manually set your existing API key.");
        }
        if (args.length != 2) {
            throw new WrongUsageException("/" + getCommandName() + " stalk <playerName>");
        } else if (Utils.isInvalidMcName(args[1])) {
            throw new InvalidPlayerNameException(args[1]);
        } else {
            String playerName = args[1];
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
                        throw new ApiContactException("Mojang", "couldn't stalk " + EnumChatFormatting.DARK_RED + playerName);
                    } else if (stalkedPlayer.equals(Friend.FRIEND_NOT_FOUND)) {
                        throw new PlayerNotFoundException("There is no player with the name " + EnumChatFormatting.DARK_RED + playerName + EnumChatFormatting.RED + ".");
                    } else {
                        // ... then stalk the player
                        stalkPlayer(stalkedPlayer);
                    }
                });
            }
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
                    ApiUtils.fetchHyPlayerDetails(stalkedPlayer, hyPlayerData -> {
                        if (hyPlayerData == null) {
                            throw new ApiContactException("Hypixel", "couldn't stalk " + EnumChatFormatting.DARK_RED + stalkedPlayer.getName() + EnumChatFormatting.RED + " but they appear to be offline currently.");
                        } else if (hyPlayerData.hasNeverJoinedHypixel()) {
                            main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD + stalkedPlayer.getName() + EnumChatFormatting.YELLOW + " has " + EnumChatFormatting.GOLD + "never " + EnumChatFormatting.YELLOW + "been on Hypixel (or might be nicked).");
                        } else if (hyPlayerData.isHidingOnlineStatus()) {
                            main.getChatHelper().sendMessage(new ChatComponentText(hyPlayerData.getPlayerNameFormatted()).appendSibling(new ChatComponentText(" is hiding their online status from the Hypixel API. You can see their online status with ").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW)))
                                    .appendSibling(new ChatComponentText("/profile " + hyPlayerData.getPlayerName()).setChatStyle(new ChatStyle()
                                            .setColor(EnumChatFormatting.GOLD)
                                            .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/profile " + hyPlayerData.getPlayerName()))
                                            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Run " + EnumChatFormatting.GOLD + "/profile " + hyPlayerData.getPlayerName())))))
                                    .appendSibling(new ChatComponentText(" while you're in a lobby (tooltip of the player head on the top left).").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW))));
                        } else if (hyPlayerData.hasNeverLoggedOut()) {
                            Pair<String, String> lastOnline = Utils.getDurationAsWords(hyPlayerData.getLastLogin());

                            main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, hyPlayerData.getPlayerNameFormatted() + EnumChatFormatting.YELLOW + " was last online " + EnumChatFormatting.GOLD + lastOnline.first() + EnumChatFormatting.YELLOW + " ago"
                                    + (lastOnline.second() != null ? " (" + EnumChatFormatting.GOLD + lastOnline.second() + EnumChatFormatting.YELLOW + ")" : "") + ".");
                        } else if (hyPlayerData.getLastLogin() > hyPlayerData.getLastLogout()) {
                            // player is logged in but is hiding their session details from API (My Profile > API settings > Online Status)
                            main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD + hyPlayerData.getPlayerNameFormatted() + EnumChatFormatting.YELLOW + " is currently playing " + EnumChatFormatting.GOLD + hyPlayerData.getLastGame() + "\n" + EnumChatFormatting.DARK_GRAY + "(" + hyPlayerData.getPlayerName() + " hides their session details from the API so that only their current game mode is visible)");
                        } else {
                            Pair<String, String> lastOnline = Utils.getDurationAsWords(hyPlayerData.getLastLogout());

                            main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, hyPlayerData.getPlayerNameFormatted() + EnumChatFormatting.YELLOW + " is " + EnumChatFormatting.GOLD + "offline" + EnumChatFormatting.YELLOW + " for " + EnumChatFormatting.GOLD + lastOnline.first() + EnumChatFormatting.YELLOW
                                    + ((lastOnline.second() != null || hyPlayerData.getLastGame() != null) ? (" ("
                                    + (lastOnline.second() != null ? EnumChatFormatting.GOLD + lastOnline.second() + EnumChatFormatting.YELLOW : "") // = last online date
                                    + (lastOnline.second() != null && hyPlayerData.getLastGame() != null ? "; " : "") // = delimiter
                                    + (hyPlayerData.getLastGame() != null ? "last played gamemode: " + EnumChatFormatting.GOLD + hyPlayerData.getLastGame() + EnumChatFormatting.YELLOW : "") // = last gamemode
                                    + ")") : "") + ".");
                        }
                    });
                }
            } else {
                String cause = (hyStalking != null) ? hyStalking.getCause() : null;
                throw new ApiContactException("Hypixel", "couldn't stalk " + EnumChatFormatting.DARK_RED + stalkedPlayer.getName() + EnumChatFormatting.RED + (cause != null ? " (Reason: " + EnumChatFormatting.DARK_RED + cause + EnumChatFormatting.RED + ")" : "") + ".");
            }
        });
    }

    private void handleBestFriendAdd(String[] args) throws CommandException {
        if (args.length != 2) {
            throw new WrongUsageException("/" + getCommandName() + " add <playerName>");
        } else if (Utils.isInvalidMcName(args[1])) {
            throw new InvalidPlayerNameException(args[1]);
        } else if (main.getFriendsHandler().isBestFriend(args[1], true)) {
            throw new MooCommandException(EnumChatFormatting.DARK_RED + args[1] + EnumChatFormatting.RED + " is a best friend already.");
        } else if (main.getFriendsHandler().getBestFriends().size() >= 100) {
            throw new MooCommandException(EnumChatFormatting.RED + "The best friends list is limited to 100 players. Remove some with " + EnumChatFormatting.WHITE + "/" + getCommandName() + " remove <name> " + EnumChatFormatting.RED + "first");
        } else {
            main.getChatHelper().sendMessage(EnumChatFormatting.GOLD, "Fetching " + EnumChatFormatting.YELLOW + args[1] + EnumChatFormatting.GOLD + "'s unique user id. This may take a few seconds...");
            // add friend async
            main.getFriendsHandler().addBestFriend(args[1]);
        }
    }

    private void handleBestFriendRemove(String[] args) throws CommandException {
        if (args.length != 2) {
            throw new WrongUsageException("/" + getCommandName() + " remove <playerName>");
        } else if (Utils.isInvalidMcName(args[1])) {
            throw new InvalidPlayerNameException(args[1]);
        }
        String username = args[1];
        boolean removed = main.getFriendsHandler().removeBestFriend(username);
        if (removed) {
            main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "Removed " + EnumChatFormatting.DARK_GREEN + username + EnumChatFormatting.GREEN + " from best friends list.");
        } else {
            throw new MooCommandException(EnumChatFormatting.DARK_RED + username + EnumChatFormatting.RED + " isn't a best friend.");
        }
    }

    private void handleListBestFriends() {
        Set<String> bestFriends = main.getFriendsHandler().getBestFriends();

        // TODO show fancy gui with list of best friends; maybe with buttons to delete them
        main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "➜ Best friends"
                + (bestFriends.isEmpty() ? "" : " (" + EnumChatFormatting.DARK_GREEN + bestFriends.size() + EnumChatFormatting.GREEN + ")") + ": "
                + ((bestFriends.isEmpty())
                ? EnumChatFormatting.ITALIC + "none :c"
                : EnumChatFormatting.DARK_GREEN + String.join(EnumChatFormatting.GREEN + ", " + EnumChatFormatting.DARK_GREEN, bestFriends)));
    }

    private void handleBestFriendsOnlineCheck() throws MooCommandException {
        if (!CredentialStorage.isMooValid) {
            throw new MooCommandException("You haven't set your Hypixel API key yet or the API key is invalid. Use " + EnumChatFormatting.DARK_RED + "/api new" + EnumChatFormatting.RED + " to request a new API key from Hypixel or use " + EnumChatFormatting.DARK_RED + "/" + this.getCommandName() + " apikey <key>" + EnumChatFormatting.RED + " to manually set your existing API key.");
        }
        if (main.getFriendsHandler().getBestFriends().size() > 0) {
            main.getChatHelper().sendMessage(EnumChatFormatting.GRAY, "Checking online status of " + EnumChatFormatting.WHITE + main.getFriendsHandler().getBestFriends().size() + EnumChatFormatting.GRAY + " best friends. This may take a few seconds.");
            main.getFriendsHandler().runBestFriendsOnlineCheck(true);
        } else {
            main.getChatHelper().sendMessage(EnumChatFormatting.RED, "You haven't added anyone to your best friends list yet. Do so with " + EnumChatFormatting.WHITE + "/moo add <playerName>");
        }
    }

    private void handleNameChangeCheck(String[] args) throws CommandException {
        if (args.length != 2) {
            throw new WrongUsageException("/" + getCommandName() + " nameChangeCheck <playerName>");
        } else if (Utils.isInvalidMcName(args[1])) {
            throw new InvalidPlayerNameException(args[1]);
        }
        Friend bestFriend = main.getFriendsHandler().getBestFriend(args[1]);
        if (bestFriend.equals(Friend.FRIEND_NOT_FOUND)) {
            throw new MooCommandException(EnumChatFormatting.DARK_RED + args[1] + EnumChatFormatting.RED + " isn't a best friend.");
        } else {
            main.getChatHelper().sendMessage(EnumChatFormatting.GOLD, "Checking if " + bestFriend.getName() + " changed their name... This will take a few seconds...");
            // check for name change async
            main.getFriendsHandler().doBestFriendNameChangeCheck(bestFriend, true);
        }
    }
    //endregion

    //region sub commands: SkyBlock
    private void handleStalkingSkyBlock(String[] args) throws CommandException {
        if (!CredentialStorage.isMooValid) {
            throw new MooCommandException("You haven't set your Hypixel API key yet or the API key is invalid. Use " + EnumChatFormatting.DARK_RED + "/api new" + EnumChatFormatting.RED + " to request a new API key from Hypixel or use " + EnumChatFormatting.DARK_RED + "/" + this.getCommandName() + " apikey <key>" + EnumChatFormatting.RED + " to manually set your existing API key.");
        }
        if (args.length != 2) {
            throw new WrongUsageException("/" + getCommandName() + " skyblockstalk <playerName>");
        } else if (Utils.isInvalidMcName(args[1])) {
            throw new InvalidPlayerNameException(args[1]);
        } else {
            String playerName = args[1];
            main.getChatHelper().sendMessage(EnumChatFormatting.GRAY, "Stalking " + EnumChatFormatting.WHITE + playerName + EnumChatFormatting.GRAY + "'s SkyBlock stats. This may take a few seconds.");
            boolean isBestFriend = main.getFriendsHandler().isBestFriend(playerName, true);
            if (isBestFriend) {
                Friend stalkedPlayer = main.getFriendsHandler().getBestFriend(playerName);
                // we have the uuid already, so stalk the player
                stalkSkyBlockStats(stalkedPlayer);
            } else {
                // fetch player uuid
                ApiUtils.fetchFriendData(playerName, stalkedPlayer -> {
                    if (stalkedPlayer == null) {
                        throw new ApiContactException("Mojang", "couldn't stalk " + EnumChatFormatting.DARK_RED + playerName);
                    } else if (stalkedPlayer.equals(Friend.FRIEND_NOT_FOUND)) {
                        throw new PlayerNotFoundException("There is no player with the name " + EnumChatFormatting.DARK_RED + playerName + EnumChatFormatting.RED + ".");
                    } else {
                        // ... then stalk the player
                        stalkSkyBlockStats(stalkedPlayer);
                    }
                });
            }
        }
    }

    private void stalkSkyBlockStats(Friend stalkedPlayer) {
        ApiUtils.fetchSkyBlockStats(stalkedPlayer, hySBStalking -> {
            if (hySBStalking != null && hySBStalking.isSuccess()) {
                HySkyBlockStats.Profile activeProfile = hySBStalking.getActiveProfile(stalkedPlayer.getUuid());

                if (activeProfile == null) {
                    throw new MooCommandException("Looks like " + EnumChatFormatting.DARK_RED + stalkedPlayer.getName() + EnumChatFormatting.RED + " hasn't played SkyBlock yet.");
                }

                String highestSkill = null;
                int highestLevel = -1;

                MooChatComponent skillLevels = new MooChatComponent("Skill levels:").gold();
                HySkyBlockStats.Profile.Member member = activeProfile.getMember(stalkedPlayer.getUuid());
                int skillLevelsSum = 0;
                for (Map.Entry<XpTables.Skill, Integer> entry : member.getSkills().entrySet()) {
                    String skill = Utils.fancyCase(entry.getKey().name());
                    int level = entry.getValue();
                    String skillLevel = MooConfig.useRomanNumerals() ? Utils.convertArabicToRoman(level) : String.valueOf(level);
                    skillLevels.appendFreshSibling(new MooChatComponent.KeyValueTooltipComponent(skill, skillLevel));

                    if (level > highestLevel) {
                        highestSkill = skill;
                        highestLevel = level;
                    }
                    if (!skill.equals("Carpentry") && !skill.equals("Runecrafting")) {
                        skillLevelsSum += level;
                    }
                }

                // output inspired by /profiles hover

                // coins:
                String coinsBankAndPurse = (activeProfile.getCoinBank() >= 0) ? Utils.formatNumberWithAbbreviations(activeProfile.getCoinBank() + member.getCoinPurse()) : Utils.formatNumberWithAbbreviations(member.getCoinPurse()) + " - purse only, bank API access disabled";
                Pair<String, String> fancyFirstJoined = member.getFancyFirstJoined();

                MooChatComponent wealthHover = new MooChatComponent("Accessible coins:").gold()
                        .appendFreshSibling(new MooChatComponent.KeyValueTooltipComponent("Purse", Utils.formatNumberWithAbbreviations(member.getCoinPurse())))
                        .appendFreshSibling(new MooChatComponent.KeyValueTooltipComponent("Bank", (activeProfile.getCoinBank() != -1) ? Utils.formatNumberWithAbbreviations(activeProfile.getCoinBank()) : "API access disabled"));
                if (activeProfile.coopCount() > 0) {
                    wealthHover.appendFreshSibling(new ChatComponentText(" "));
                    wealthHover.appendFreshSibling(new MooChatComponent.KeyValueTooltipComponent("Co-op members", String.valueOf(activeProfile.coopCount())));
                    wealthHover.appendFreshSibling(new MooChatComponent.KeyValueTooltipComponent("Co-ops' purses sum", Utils.formatNumberWithAbbreviations(activeProfile.getCoopCoinPurses(stalkedPlayer.getUuid()))));
                }

                String gameModeIcon = activeProfile.getGameModeIcon();
                MooChatComponent sbStats = new MooChatComponent("SkyBlock stats of " + stalkedPlayer.getName() + EnumChatFormatting.RESET + EnumChatFormatting.GRAY + " (" + (gameModeIcon.isEmpty() ? "" : EnumChatFormatting.getTextWithoutFormattingCodes(gameModeIcon) + ", ") + activeProfile.getCuteName() + ")").gold().bold().setUrl("https://sky.shiiyu.moe/stats/" + stalkedPlayer.getName() + "/" + activeProfile.getCuteName(), "Click to view SkyBlock stats on sky.shiiyu.moe")
                        .appendFreshSibling(new MooChatComponent.KeyValueChatComponent("Coins", coinsBankAndPurse).setHover(wealthHover));
                // highest skill + skill average:
                if (highestSkill != null) {
                    if (highestLevel == 0) {
                        sbStats.appendFreshSibling(new MooChatComponent.KeyValueChatComponent("Highest Skill", "All skills level 0"));
                    } else {
                        String highestSkillLevel = MooConfig.useRomanNumerals() ? Utils.convertArabicToRoman(highestLevel) : String.valueOf(highestLevel);
                        sbStats.appendFreshSibling(new MooChatComponent.KeyValueChatComponent("Highest Skill", highestSkill + " " + highestSkillLevel).setHover(skillLevels));
                    }
                    double skillAverage = XpTables.Skill.getSkillAverage(skillLevelsSum);
                    sbStats.appendFreshSibling(new MooChatComponent.KeyValueChatComponent("Skill average", String.format("%.1f", skillAverage))
                            .setHover(new MooChatComponent("Average skill level over all non-cosmetic skills\n(all except Carpentry and Runecrafting)").gray()));
                } else {
                    sbStats.appendFreshSibling(new MooChatComponent.KeyValueChatComponent("Highest Skill", "API access disabled"));
                }

                // slayer levels:
                StringBuilder slayerLevels = new StringBuilder();
                StringBuilder slayerLevelsTooltip = new StringBuilder();
                MooChatComponent slayerLevelsTooltipComponent = new MooChatComponent("Slayer bosses:").gold();
                for (Map.Entry<XpTables.Slayer, Integer> entry : member.getSlayerLevels().entrySet()) {
                    String slayerBoss = Utils.fancyCase(entry.getKey().name());
                    if (slayerLevels.length() > 0) {
                        slayerLevels.append(EnumChatFormatting.GRAY).append(" | ").append(EnumChatFormatting.YELLOW);
                        slayerLevelsTooltip.append(EnumChatFormatting.DARK_GRAY).append(" | ").append(EnumChatFormatting.WHITE);
                    }
                    slayerLevelsTooltip.append(slayerBoss);
                    int level = entry.getValue();

                    String slayerLevel = (level > 0) ? (MooConfig.useRomanNumerals() ? Utils.convertArabicToRoman(level) : String.valueOf(level)) : "0";
                    slayerLevels.append(slayerLevel);
                }
                MooChatComponent slayerLevelsComponent = new MooChatComponent.KeyValueChatComponent("Slayer levels", slayerLevels.toString());
                slayerLevelsComponent.setHover(slayerLevelsTooltipComponent.appendFreshSibling(new MooChatComponent(slayerLevelsTooltip.toString()).white()));
                sbStats.appendFreshSibling(slayerLevelsComponent);

                // dungeons:
                MooChatComponent dungeonsComponent = null;
                HySkyBlockStats.Profile.Dungeons dungeons = member.getDungeons();
                boolean hasPlayedDungeons = dungeons != null && dungeons.hasPlayed();
                if (hasPlayedDungeons) {
                    MooChatComponent dungeonHover = new MooChatComponent("Dungeoneering").gold().bold();

                    DataHelper.DungeonClass selectedClass = dungeons.getSelectedClass();
                    String selectedDungClass = "" + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + "no class selected";
                    if (selectedClass != null) {
                        int selectedClassLevel = dungeons.getSelectedClassLevel();
                        selectedDungClass = selectedClass.getName() + " " + (MooConfig.useRomanNumerals() ? Utils.convertArabicToRoman(selectedClassLevel) : selectedClassLevel);
                    }
                    dungeonsComponent = new MooChatComponent.KeyValueChatComponent("Dungeoneering", selectedDungClass)
                            .setHover(dungeonHover);


                    // for each class (Archer, Berserk, ...)
                    Map<DataHelper.DungeonClass, Integer> classLevels = dungeons.getClassLevels();
                    if (classLevels != null && !classLevels.isEmpty()) {
                        dungeonHover.appendFreshSibling(new MooChatComponent("Classes:").gold());
                        for (Map.Entry<DataHelper.DungeonClass, Integer> classEntry : classLevels.entrySet()) {
                            String classLevel = (MooConfig.useRomanNumerals() ? Utils.convertArabicToRoman(classEntry.getValue()) : String.valueOf(classEntry.getValue()));
                            dungeonHover.appendFreshSibling(new MooChatComponent.KeyValueChatComponent((classEntry.getKey() == selectedClass ? "➜ " : "   ") + classEntry.getKey().getName(), classLevel));
                        }
                    }

                    // for each dungeon type (Catacombs, ...)
                    Map<String, HySkyBlockStats.Profile.Dungeons.Type> dungeonTypes = dungeons.getDungeonTypes();
                    if (dungeonTypes != null && !dungeonTypes.isEmpty()) {
                        // for each dungeon type: catacombs, ...
                        for (Map.Entry<String, HySkyBlockStats.Profile.Dungeons.Type> dungeonTypeEntry : dungeonTypes.entrySet()) {
                            // dungeon type entry for chat
                            HySkyBlockStats.Profile.Dungeons.Type dungeonType = dungeonTypeEntry.getValue();
                            if (!dungeonType.hasPlayed()) {
                                // never played this dungeon type
                                continue;
                            }
                            String dungeonTypeName = Utils.fancyCase(dungeonTypeEntry.getKey());
                            boolean isMasterFloor = dungeonTypeName.startsWith("Master ");
                            dungeonsComponent.appendFreshSibling(new MooChatComponent.KeyValueChatComponent("   " + dungeonTypeName, dungeonType.getSummary(isMasterFloor)))
                                    .setHover(dungeonHover);
                            // dungeon type entry for tooltip
                            if (isMasterFloor) {
                                dungeonHover.appendFreshSibling(new MooChatComponent(dungeonTypeName).gold());
                            } else {
                                // non-master dungeon
                                int dungeonTypeLevel = dungeonTypeEntry.getValue().getLevel();
                                dungeonHover.appendFreshSibling(new MooChatComponent.KeyValueChatComponent(dungeonTypeName, "Level " + (MooConfig.useRomanNumerals() ? Utils.convertArabicToRoman(dungeonTypeLevel) : dungeonTypeLevel)));
                            }

                            // for each floor
                            SortedMap<String, StringBuilder> floorStats = new TreeMap<>();
                            // ... add completed floors:
                            if (dungeonType.getTierCompletions() != null) {
                                for (Map.Entry<String, Integer> floorCompletions : dungeonType.getTierCompletions().entrySet()) {
                                    StringBuilder floorSummary = new StringBuilder();
                                    floorStats.put(floorCompletions.getKey(), floorSummary);
                                    // completed floor count:
                                    floorSummary.append(floorCompletions.getValue());
                                }
                            }
                            // ... add played floors
                            Map<String, Integer> dungeonTypeTimesPlayed = dungeonType.getTimesPlayed();
                            if (dungeonTypeTimesPlayed != null) {
                                for (Map.Entry<String, Integer> floorPlayed : dungeonTypeTimesPlayed.entrySet()) {
                                    StringBuilder floorSummary = floorStats.get(floorPlayed.getKey());
                                    if (floorSummary == null) {
                                        // hasn't beaten this floor, but already attempted it
                                        floorSummary = new StringBuilder("0");
                                        floorStats.put(floorPlayed.getKey(), floorSummary);
                                    }
                                    // played floor count:
                                    floorSummary.append(EnumChatFormatting.DARK_GRAY).append(" / ").append(EnumChatFormatting.YELLOW).append(floorPlayed.getValue());
                                }
                            } else {
                                // missing value for attempted floors, only show completed floors
                                for (StringBuilder floorSummary : floorStats.values()) {
                                    floorSummary.append(EnumChatFormatting.DARK_GRAY).append(" / ").append(EnumChatFormatting.YELLOW).append(EnumChatFormatting.OBFUSCATED).append("#");
                                }
                            }
                            // ... add best scores
                            if (dungeonType.getBestScore() != null) {
                                for (Map.Entry<String, Integer> bestScores : dungeonType.getBestScore().entrySet()) {
                                    StringBuilder floorSummary = floorStats.getOrDefault(bestScores.getKey(), new StringBuilder());
                                    // best floor score:
                                    floorSummary.append(EnumChatFormatting.DARK_GRAY).append(" (").append(EnumChatFormatting.WHITE).append(bestScores.getValue()).append(EnumChatFormatting.DARK_GRAY).append(")");
                                }
                            }

                            // add floor stats to dungeon type:
                            for (Map.Entry<String, StringBuilder> floorInfo : floorStats.entrySet()) {
                                dungeonHover.appendFreshSibling(new MooChatComponent.KeyValueChatComponent("   Floor " + floorInfo.getKey(), floorInfo.getValue().toString()));
                            }
                        }
                        dungeonHover.appendFreshSibling(new MooChatComponent(" Floor nr: completed / total floors (best score)").gray().italic());
                    }
                }
                if (!hasPlayedDungeons) {
                    dungeonsComponent = new MooChatComponent.KeyValueChatComponent("Dungeons", EnumChatFormatting.ITALIC + "never played");
                }
                sbStats.appendFreshSibling(dungeonsComponent);

                // pets:
                Pet activePet = null;
                Pet bestPet = null;
                StringBuilder pets = new StringBuilder();
                List<Pet> memberPets = member.getPets();
                int showPetsLimit = Math.min(16, memberPets.size());
                for (int i = 0; i < showPetsLimit; i++) {
                    Pet pet = memberPets.get(i);
                    if (pet.isActive()) {
                        activePet = pet;
                    } else {
                        if (activePet == null && bestPet == null && pets.length() == 0) {
                            // no active pet, display highest pet instead
                            bestPet = pet;
                            continue;
                        } else if (pets.length() > 0) {
                            pets.append("\n");
                        }
                        pets.append(pet.toFancyString());
                    }
                }
                int remainingPets = memberPets.size() - showPetsLimit;
                if (remainingPets > 0 && pets.length() > 0) {
                    pets.append("\n").append(EnumChatFormatting.GRAY).append(" + ").append(remainingPets).append(" other pets");
                }
                MooChatComponent petsComponent = null;
                if (activePet != null) {
                    petsComponent = new MooChatComponent.KeyValueChatComponent("Active Pet", activePet.toFancyString());
                } else if (bestPet != null) {
                    petsComponent = new MooChatComponent.KeyValueChatComponent("Best Pet", bestPet.toFancyString());
                }
                if (pets.length() > 0 && petsComponent != null) {
                    petsComponent.setHover(new MooChatComponent("Other pets:").gold().bold().appendFreshSibling(new MooChatComponent(pets.toString())));
                }
                if (petsComponent == null) {
                    petsComponent = new MooChatComponent.KeyValueChatComponent("Pet", "none");
                }
                sbStats.appendFreshSibling(petsComponent);

                // minions:
                Pair<Integer, Integer> uniqueMinionsData = activeProfile.getUniqueMinions();
                String uniqueMinions = String.valueOf(uniqueMinionsData.first());
                String uniqueMinionsHoverText = null;
                if (uniqueMinionsData.second() > activeProfile.coopCount()) {
                    // all players have their unique minions api access disabled
                    uniqueMinions = "API access disabled";
                } else if (uniqueMinionsData.second() > 0) {
                    // at least one player has their unique minions api access disabled
                    uniqueMinions += EnumChatFormatting.GRAY + " or more";
                    uniqueMinionsHoverText = "" + EnumChatFormatting.WHITE + uniqueMinionsData.second() + " out of " + (activeProfile.coopCount() + 1) + EnumChatFormatting.GRAY + " Co-op members have disabled API access, so some unique minions may be missing";
                }

                MooChatComponent.KeyValueChatComponent uniqueMinionsComponent = new MooChatComponent.KeyValueChatComponent("Unique Minions", uniqueMinions);
                if (uniqueMinionsHoverText != null) {
                    uniqueMinionsComponent.setHover(new MooChatComponent(uniqueMinionsHoverText).gray());
                }
                sbStats.appendFreshSibling(uniqueMinionsComponent);
                // fairy souls:
                sbStats.appendFreshSibling(new MooChatComponent.KeyValueChatComponent("Fairy Souls", (member.getFairySoulsCollected() >= 0) ? String.valueOf(member.getFairySoulsCollected()) : "API access disabled"));
                // profile age:
                sbStats.appendFreshSibling(new MooChatComponent.KeyValueChatComponent("Profile age", fancyFirstJoined.first()).setHover(new MooChatComponent.KeyValueTooltipComponent("Join date", (fancyFirstJoined.second() == null ? "today" : fancyFirstJoined.second()))));
                // last save:
                Pair<String, String> fancyLastSave = member.getFancyLastSave();
                sbStats.appendFreshSibling(new MooChatComponent.KeyValueChatComponent("Last save", fancyLastSave.first() + " ago").setHover(new MooChatComponent.KeyValueTooltipComponent("Last save", (fancyLastSave.second() == null ? "today" : fancyLastSave.second()))
                        .appendFreshSibling(new MooChatComponent("= last time " + stalkedPlayer.getName() + " has played SkyBlock.").white())));

                main.getChatHelper().sendMessage(sbStats);
            } else {
                String cause = (hySBStalking != null) ? hySBStalking.getCause() : null;
                String reason = "";
                if (cause != null) {
                    reason = " (Reason: " + EnumChatFormatting.DARK_RED + cause + EnumChatFormatting.RED + ")";
                }
                throw new ApiContactException("Hypixel", "couldn't stalk " + EnumChatFormatting.DARK_RED + stalkedPlayer.getName() + EnumChatFormatting.RED + reason + ".");
            }
        });
    }

    private void handleAnalyzeChests(String[] args) {
        if (args.length == 1) {
            boolean enabledChestTracker = main.enableChestTracker();
            if (enabledChestTracker) {
                // chest tracker wasn't enabled before, now it is
                String analyzeCommand = "/" + getCommandName() + " analyzeChests";
                if (MooConfig.chestAnalyzerShowCommandUsage) {
                    main.getChatHelper().sendMessage(new MooChatComponent("Enabled chest tracker! You can now...").green()
                            .appendFreshSibling(new MooChatComponent(EnumChatFormatting.GREEN + "  ❶ " + EnumChatFormatting.YELLOW + "add chests on your island by opening them; deselect chests by Sneaking + Right Click.").yellow())
                            .appendFreshSibling(new MooChatComponent(EnumChatFormatting.GREEN + "  ❷ " + EnumChatFormatting.YELLOW + "use " + EnumChatFormatting.GOLD + analyzeCommand + EnumChatFormatting.YELLOW + " again to run the chest analysis.").yellow().setSuggestCommand(analyzeCommand))
                            .appendFreshSibling(new MooChatComponent("     (You can search for an item inside your chests by double clicking its analysis row)").gray().setSuggestCommand(analyzeCommand))
                            .appendFreshSibling(new MooChatComponent(EnumChatFormatting.GREEN + "  ❸ " + EnumChatFormatting.YELLOW + "use " + EnumChatFormatting.GOLD + analyzeCommand + " stop" + EnumChatFormatting.YELLOW + " to stop the chest tracker and clear current results.").yellow().setSuggestCommand(analyzeCommand + " stop")));
                } else {
                    main.getChatHelper().sendMessage(new MooChatComponent("Enabled chest tracker! " + EnumChatFormatting.GRAY + "Run " + analyzeCommand + " again to run the chest analysis.").green().setSuggestCommand(analyzeCommand));
                }
            } else {
                // chest tracker was already enabled, open analysis GUI
                main.getChestTracker().analyzeResults();
                new TickDelay(() -> Minecraft.getMinecraft().displayGuiScreen(new ChestOverviewGui(main)), 1);
            }
        } else if (args.length == 2 && args[1].equalsIgnoreCase("stop")) {
            boolean disabledChestTracker = main.disableChestTracker();
            if (disabledChestTracker) {
                main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "Disabled chest tracker and cleared chest cache!");
            } else {
                main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, "Chest tracker wasn't even enabled...");
            }
        } else {
            String analyzeCommand = "/" + getCommandName() + " analyzeChests";
            main.getChatHelper().sendMessage(new MooChatComponent(Cowlection.MODNAME + " chest tracker & analyzer").gold().bold()
                    .appendFreshSibling(new MooChatComponent("Use " + EnumChatFormatting.GOLD + analyzeCommand + EnumChatFormatting.YELLOW + " to start tracking chests on your island! " + EnumChatFormatting.GREEN + "Then you can...").yellow().setSuggestCommand(analyzeCommand))
                    .appendFreshSibling(new MooChatComponent(EnumChatFormatting.GREEN + "  ❶ " + EnumChatFormatting.YELLOW + "add chests by opening them; deselect chests by Sneaking + Right Click.").yellow())
                    .appendFreshSibling(new MooChatComponent(EnumChatFormatting.GREEN + "  ❷ " + EnumChatFormatting.YELLOW + "use " + EnumChatFormatting.GOLD + analyzeCommand + EnumChatFormatting.YELLOW + " again to run the chest analysis.").yellow().setSuggestCommand(analyzeCommand))
                    .appendFreshSibling(new MooChatComponent(EnumChatFormatting.GREEN + "  ❸ " + EnumChatFormatting.YELLOW + "use " + EnumChatFormatting.GOLD + analyzeCommand + " stop" + EnumChatFormatting.YELLOW + " to stop the chest tracker and clear current results.").yellow().setSuggestCommand(analyzeCommand + " stop")));
        }
    }

    private void handleAnalyzeIsland(ICommandSender sender) {
        Map<String, String> minions = DataHelper.getMinions();

        Map<String, Integer> detectedMinions = new HashMap<>();
        Map<Integer, Integer> detectedMinionsWithSkin = new HashMap<>();
        int detectedMinionCount = 0;
        int minionsWithSkinCount = 0;
        entityLoop:
        for (Entity entity : sender.getEntityWorld().loadedEntityList) {
            if (entity instanceof EntityArmorStand) {
                EntityArmorStand minion = (EntityArmorStand) entity;

                if (minion.isInvisible() || !minion.isSmall() || minion.getHeldItem() == null) {
                    // not a minion: invisible, or not small armor stand, or no item in hand (= minion in a minion chair)
                    continue;
                }
                for (int slot = 0; slot < 4; slot++) {
                    if (minion.getCurrentArmor(slot) == null) {
                        // not a minion: missing equipment
                        continue entityLoop;
                    }
                }
                ItemStack skullItem = minion.getCurrentArmor(3); // head slot
                if (skullItem.getItem() instanceof ItemSkull && skullItem.getMetadata() == 3 && skullItem.hasTagCompound()) {
                    // is a player head!
                    if (skullItem.getTagCompound().hasKey("SkullOwner", Constants.NBT.TAG_COMPOUND)) {
                        NBTTagCompound skullOwner = skullItem.getTagCompound().getCompoundTag("SkullOwner");
                        String skullDataBase64 = skullOwner.getCompoundTag("Properties").getTagList("textures", Constants.NBT.TAG_COMPOUND).getCompoundTagAt(0).getString("Value");
                        String skullData = new String(Base64.decodeBase64(skullDataBase64));
                        String minionSkinId = StringUtils.substringBetween(skullData, "http://textures.minecraft.net/texture/", "\"");
                        String detectedMinion = minions.get(minionSkinId);
                        if (detectedMinion != null) {
                            // minion head matches one know minion tier
                            detectedMinions.put(detectedMinion, detectedMinions.getOrDefault(detectedMinion, 0) + 1);
                            detectedMinionCount++;
                        } else {
                            int minionTier = ImageUtils.getTierFromTexture(minionSkinId);
                            if (minionTier > 0) {
                                detectedMinionsWithSkin.put(minionTier, detectedMinionsWithSkin.getOrDefault(minionTier, 0) + 1);
                                minionsWithSkinCount++;
                            } else {
                                // looked like a minion but has no matching tier badge
                                main.getLogger().info("[/moo analyzeIsland] Found an armor stand that could be a minion but it is missing a tier badge: " + minionSkinId + "\t\t\t" + minion.serializeNBT());
                            }
                        }
                    }
                }
            }
        }
        StringBuilder analysisResults = new StringBuilder("Found ").append(EnumChatFormatting.GOLD).append(detectedMinionCount).append(EnumChatFormatting.YELLOW).append(" minions");
        if (minionsWithSkinCount > 0) {
            analysisResults.append(" + ").append(EnumChatFormatting.GOLD).append(minionsWithSkinCount).append(EnumChatFormatting.YELLOW).append(" unknown minions with skins");
        }
        analysisResults.append(" on this island");
        detectedMinions.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // sort alphabetically by minion name and tier
                .forEach(minion -> {
                    String minionWithTier = minion.getKey();
                    int lastSpace = minionWithTier.lastIndexOf(' ');

                    String tierRoman = minionWithTier.substring(lastSpace + 1);

                    int tierArabic = Utils.convertRomanToArabic(tierRoman);
                    EnumChatFormatting tierColor = Utils.getMinionTierColor(tierArabic);

                    minionWithTier = minionWithTier.substring(0, lastSpace) + " " + tierColor + (MooConfig.useRomanNumerals() ? tierRoman : tierArabic);
                    analysisResults.append("\n  ").append(EnumChatFormatting.GOLD).append(minion.getValue()).append(minion.getValue() > 1 ? "✕ " : "⨉ ")
                            .append(EnumChatFormatting.YELLOW).append(minionWithTier);
                });
        detectedMinionsWithSkin.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // sort by tier
                .forEach(minionWithSkin -> {
                    EnumChatFormatting tierColor = Utils.getMinionTierColor(minionWithSkin.getKey());
                    String minionTier = MooConfig.useRomanNumerals() ? Utils.convertArabicToRoman(minionWithSkin.getKey()) : String.valueOf(minionWithSkin.getKey());
                    analysisResults.append("\n  ").append(EnumChatFormatting.GOLD).append(minionWithSkin.getValue()).append(minionWithSkin.getValue() > 1 ? "✕ " : "⨉ ")
                            .append(EnumChatFormatting.RED).append("Unknown minion ").append(EnumChatFormatting.YELLOW).append("(new or with minion skin) ").append(tierColor).append(minionTier);
                });
        main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, analysisResults.toString());
    }

    private void handleWhatAmILookingAt(ICommandSender sender, boolean showAllInfo) {
        MovingObjectPosition lookingAt = Minecraft.getMinecraft().objectMouseOver;
        if (lookingAt != null) {
            switch (lookingAt.typeOfHit) {
                case BLOCK: {
                    TileEntity te = sender.getEntityWorld().getTileEntity(lookingAt.getBlockPos());
                    if (te instanceof TileEntitySkull) {
                        TileEntitySkull skull = (TileEntitySkull) te;
                        if (skull.getSkullType() != 3) {
                            // non-player skull, abort!
                            break;
                        }
                        NBTTagCompound nbt = new NBTTagCompound();
                        skull.writeToNBT(nbt);
                        // is a player head!
                        if (nbt.hasKey("Owner", Constants.NBT.TAG_COMPOUND)) {
                            NBTTagCompound relevantNbt = tldrInfo(nbt, showAllInfo);
                            BlockPos skullPos = skull.getPos();
                            relevantNbt.setTag("__position", new NBTTagIntArray(new int[]{skullPos.getX(), skullPos.getY(), skullPos.getZ()}));
                            Utils.copyToClipboardOrSaveAsFile("skull data", "skull", relevantNbt, true);
                            return;
                        }
                    } else if (te instanceof TileEntitySign) {
                        TileEntitySign sign = (TileEntitySign) te;
                        NBTTagCompound nbt = new NBTTagCompound();
                        for (int lineNr = 0; lineNr < sign.signText.length; lineNr++) {
                            nbt.setString("Text" + (lineNr + 1), sign.signText[lineNr].getFormattedText());
                            nbt.setString("TextUnformatted" + (lineNr + 1), sign.signText[lineNr].getUnformattedText());
                        }
                        Utils.copyToClipboardOrSaveAsFile("sign data", "sign", nbt, true);
                        return;
                    } else if (te instanceof TileEntityBanner) {
                        List<String> possiblePatterns = Arrays.asList("b", "bl", "bo", "br", "bri", "bs", "bt", "bts", "cbo", "cr", "cre", "cs", "dls", "drs", "flo", "gra", "hh", "ld", "ls", "mc", "moj", "mr", "ms", "rd", "rs", "sc", "sku", "ss", "tl", "tr", "ts", "tt", "tts", "vh", "lud", "rud", "gru", "hhb", "vhr");
                        String base64Alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+/";

                        TileEntityBanner banner = (TileEntityBanner) te;
                        Iterator<TileEntityBanner.EnumBannerPattern> bannerPatterns = banner.getPatternList().iterator();
                        Iterator<EnumDyeColor> bannerColors = banner.getColorList().iterator();
                        try {
                            // hash used by needcoolshoes.com
                            StringBuilder bannerHash = new StringBuilder();
                            while (bannerPatterns.hasNext() && bannerColors.hasNext()) {
                                int patternId = possiblePatterns.indexOf(bannerPatterns.next().getPatternID());
                                int color = bannerColors.next().getDyeDamage();
                                int first = ((patternId >> 6) << 4) | (color & 0xF);
                                int second = patternId & 0x3F;
                                bannerHash.append(base64Alphabet.charAt(first)).append(base64Alphabet.charAt(second));
                            }
                            main.getChatHelper().sendMessage(new MooChatComponent("➡ View banner on needcoolshoes.com").green().setUrl("https://www.needcoolshoes.com/banner?=" + bannerHash));
                        } catch (IndexOutOfBoundsException e) {
                            main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Failed to parse banner data (unknown banner pattern).");
                        }
                        return;
                    }
                    break;
                }
                case ENTITY: {
                    Entity entity = lookingAt.entityHit;
                    if (entity instanceof EntityArmorStand) {
                        // looking at non-invisible armor stand (e.g. Minion)
                        EntityArmorStand armorStand = (EntityArmorStand) entity;
                        copyEntityInfoToClipboard("armor stand '" + armorStand.getName() + EnumChatFormatting.GREEN + "'", "armorstand", armorStand, showAllInfo);
                        return;
                    } else if (entity instanceof EntityOtherPlayerMP) {
                        // looking at NPC or another player
                        EntityOtherPlayerMP otherPlayer = (EntityOtherPlayerMP) entity;
                        copyEntityInfoToClipboard("player/npc '" + otherPlayer.getDisplayNameString() + EnumChatFormatting.GREEN + "'", "npc_" + otherPlayer.getDisplayNameString(), otherPlayer, showAllInfo);
                        return;
                    } else if (entity instanceof EntityItemFrame) {
                        EntityItemFrame itemFrame = (EntityItemFrame) entity;

                        ItemStack displayedItem = itemFrame.getDisplayedItem();
                        if (displayedItem != null) {
                            NBTTagCompound nbt = new NBTTagCompound();
                            if (displayedItem.getItem() == Items.filled_map) {
                                // filled map
                                MapData mapData = ItemMap.loadMapData(displayedItem.getItemDamage(), sender.getEntityWorld());
                                File mapFile = ImageUtils.saveMapToFile(mapData);
                                if (mapFile != null) {
                                    main.getChatHelper().sendMessage(new MooChatComponent("Saved map as " + mapFile.getName() + " ").green().setOpenFile(mapFile).appendSibling(new MooChatComponent("[open file]").gold())
                                            .appendSibling(new MooChatComponent(" [open folder]").darkAqua().setOpenFile(mapFile.getParentFile())));
                                } else {
                                    main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Couldn't save map for some reason");
                                }
                                return;
                            } else {
                                displayedItem.writeToNBT(nbt);
                            }
                            Utils.copyToClipboardOrSaveAsFile("item in item frame '" + displayedItem.getDisplayName() + EnumChatFormatting.GREEN + "'", "itemframe-item_" + displayedItem.getDisplayName(), nbt, true);
                            return;
                        }
                    } else if (entity instanceof EntityLiving) {
                        EntityLiving living = (EntityLiving) entity;
                        copyEntityInfoToClipboard("mob '" + living.getName() + EnumChatFormatting.GREEN + "'", "mob_" + living.getName(), living, showAllInfo);
                        return;
                    }
                    break;
                }
                default:
                    // didn't find anything...
            }
        }
        // didn't find anything special; search for all nearby entities
        double maxDistance = 5; // default 4
        Entity self = sender.getCommandSenderEntity();
        Vec3 selfLook = self.getLook(1);
        float searchRadius = 1.0F;
        List<Entity> nearbyEntities = sender.getEntityWorld().getEntitiesInAABBexcluding(self, self.getEntityBoundingBox().addCoord(selfLook.xCoord * maxDistance, selfLook.yCoord * maxDistance, selfLook.zCoord * maxDistance).expand(searchRadius, searchRadius, searchRadius), entity1 -> true);

        if (nearbyEntities.size() > 0) {
            NBTTagList entities = new NBTTagList();
            for (Entity entity : nearbyEntities) {
                NBTTagCompound relevantNbt = extractEntityInfo(entity, showAllInfo);
                // add additional info to make it easier to find the correct entity in the list of entities
                relevantNbt.setTag("_entityType", new NBTTagString(entity.getClass().getSimpleName()));
                NBTTagList position = new NBTTagList();
                position.appendTag(new NBTTagDouble(entity.posX));
                position.appendTag(new NBTTagDouble(entity.posY));
                position.appendTag(new NBTTagDouble(entity.posZ));
                relevantNbt.setTag("_position", position);
                entities.appendTag(relevantNbt);
            }

            Utils.copyToClipboardOrSaveAsFile(nearbyEntities.size() + " nearby entities", "entities", entities, true);
        } else {
            main.getChatHelper().sendMessage(EnumChatFormatting.RED, "You stare into the void... and see nothing of interest. " + EnumChatFormatting.GRAY + "Try looking at: NPCs, mobs, armor stands, placed skulls, banners, signs, dropped items, item in item frames, or maps on a wall.");
        }
    }

    private NBTTagCompound extractEntityInfo(Entity entity, boolean showAllInfo) {
        NBTTagCompound nbt = new NBTTagCompound();
        entity.writeToNBT(nbt);
        NBTTagCompound relevantNbt = tldrInfo(nbt, showAllInfo);

        if (entity instanceof EntityOtherPlayerMP) {
            EntityOtherPlayerMP otherPlayer = (EntityOtherPlayerMP) entity;
            relevantNbt.setString("__name", otherPlayer.getName());
            if (otherPlayer.hasCustomName()) {
                relevantNbt.setString("__customName", otherPlayer.getCustomNameTag());
            }
            GameProfile gameProfile = otherPlayer.getGameProfile();
            for (Property property : gameProfile.getProperties().get("textures")) {
                relevantNbt.setString("_skin", property.getValue());
            }
        }
        if (entity instanceof EntityLiving || entity instanceof EntityOtherPlayerMP) {
            // either EntityLiving (any mob), or EntityOtherPlayerMP => find other nearby "name tag" EntityArmorStands
            List<Entity> nearbyArmorStands = entity.getEntityWorld().getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().expand(0.2d, 3, 0.2d), nearbyEntity -> {
                if (nearbyEntity instanceof EntityArmorStand) {
                    EntityArmorStand armorStand = (EntityArmorStand) nearbyEntity;
                    if (armorStand.isInvisible() && armorStand.hasCustomName()) {
                        for (ItemStack equipment : armorStand.getInventory()) {
                            if (equipment != null) {
                                // armor stand has equipment, abort!
                                return false;
                            }
                        }
                        // armor stand has a custom name, is invisible and has no equipment -> probably a "name tag"-armor stand
                        return true;
                    }
                }
                return false;
            });
            if (nearbyArmorStands.size() > 0) {
                nearbyArmorStands.sort(Comparator.<Entity>comparingDouble(nearbyArmorStand -> nearbyArmorStand.posY).reversed());
                NBTTagList nearbyText = new NBTTagList();
                for (int i = 0, maxNearbyArmorStands = Math.min(10, nearbyArmorStands.size()); i < maxNearbyArmorStands; i++) {
                    Entity nearbyArmorStand = nearbyArmorStands.get(i);
                    nearbyText.appendTag(new NBTTagString(nearbyArmorStand.getCustomNameTag()));
                }
                relevantNbt.setTag("__nearbyText", nearbyText);
            }
        }
        return relevantNbt;
    }

    private void copyEntityInfoToClipboard(String what, String fileName, Entity entity, boolean showAllInfo) {
        NBTTagCompound relevantNbt = extractEntityInfo(entity, showAllInfo);
        Utils.copyToClipboardOrSaveAsFile(what, fileName, relevantNbt, true);
    }

    private NBTTagCompound tldrInfo(NBTTagCompound nbt, boolean showAllInfo) {
        if (showAllInfo) {
            // don't tl;dr!
            return nbt;
        }
        String[] importantTags = new String[]{"CustomName", "id", "Damage", "Count", "Equipment", "Item", "tag", "ExtraAttributes", "Owner"};
        NBTTagCompound relevantNbt = new NBTTagCompound();
        for (String tag : importantTags) {
            if (nbt.hasKey(tag)) {
                relevantNbt.setTag(tag, nbt.getTag(tag));
            }
        }
        return relevantNbt;
    }

    private void handleDungeon(String[] args) throws MooCommandException {
        DungeonCache dungeonCache = main.getDungeonCache();
        if (args.length == 2 && args[1].equalsIgnoreCase("enter")) {
            // enter dungeon in case for some reason it wasn't detected automatically
            dungeonCache.onDungeonEnterOrLeave(true);
        } else if (args.length == 2 && args[1].equalsIgnoreCase("leave")) {
            // leave dungeon in case for some reason it wasn't detected automatically
            dungeonCache.onDungeonEnterOrLeave(false);
        } else if ((args.length == 2 && (args[1].equalsIgnoreCase("party") || args[1].equalsIgnoreCase("p")))
                || args.length == 1 && args[0].equalsIgnoreCase("dp")) {
            if (!CredentialStorage.isMooValid) {
                throw new MooCommandException("You haven't set your Hypixel API key yet or the API key is invalid. Use " + EnumChatFormatting.DARK_RED + "/api new" + EnumChatFormatting.RED + " to request a new API key from Hypixel or use " + EnumChatFormatting.DARK_RED + "/" + this.getCommandName() + " apikey <key>" + EnumChatFormatting.RED + " to manually set your existing API key.");
            } else if (dungeonsPartyListener != null) {
                throw new MooCommandException("Please wait a few seconds before using this command again.");
            }
            main.getChatHelper().sendServerCommand("/party list");
            new TickDelay(() -> {
                // abort after 10 seconds
                if (dungeonsPartyListener.isStillRunning()) {
                    dungeonsPartyListener.shutdown();
                    main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Dungeon party analysis timed out. Probably the recognition of the party members failed.");
                }
                dungeonsPartyListener = null;
            }, 10 * 20);
            // register dungeon listener
            dungeonsPartyListener = new DungeonsPartyListener(main);
        } else if (args.length == 2 && args[1].equalsIgnoreCase("rules")
                || args.length == 1 && args[0].equalsIgnoreCase("dr")) {
            displayGuiScreen(new RuleEditorGui());
        } else if (dungeonCache.isInDungeon()) {
            dungeonCache.sendDungeonPerformance();
        } else {
            throw new MooCommandException(EnumChatFormatting.DARK_RED + "Looks like you're not in a dungeon... However, you can manually enable the Dungeon Performance overlay with " + EnumChatFormatting.RED + "/" + getCommandName() + " dungeon enter" + EnumChatFormatting.DARK_RED + ". You can also force-leave a dungeon with " + EnumChatFormatting.RED + "/" + getCommandName() + " dungeon leave.\n" + EnumChatFormatting.GRAY + "Want to inspect your current party members? Use " + EnumChatFormatting.WHITE + "/" + getCommandName() + " dungeon party");
        }
    }
    //endregion

    //region sub-commands: miscellaneous
    private void handleGuiScale(String[] args) throws CommandException {
        GameSettings gameSettings = Minecraft.getMinecraft().gameSettings;
        int currentGuiScale = gameSettings.guiScale;
        if (args.length == 1) {
            main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "➜ Current GUI scale: " + EnumChatFormatting.DARK_GREEN + currentGuiScale);
        } else {
            int scale = MathHelper.parseIntWithDefault(args[1], -1);
            if (scale == -1 || scale > 10) {
                throw new NumberInvalidException(EnumChatFormatting.DARK_RED + args[1] + EnumChatFormatting.RED + " is an invalid GUI scale value. Valid values are integers below 10");
            }
            gameSettings.guiScale = scale;
            gameSettings.saveOptions();
            main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "✔ New GUI scale: " + EnumChatFormatting.DARK_GREEN + scale + EnumChatFormatting.GREEN + " (previous: " + EnumChatFormatting.DARK_GREEN + currentGuiScale + EnumChatFormatting.GREEN + ")");
        }
    }

    private void handleApiKey(String[] args) throws CommandException {
        if (args.length == 1) {
            String firstSentence;
            EnumChatFormatting color;
            EnumChatFormatting colorSecondary;
            if (CredentialStorage.isMooValid) {
                firstSentence = "[" + Cowlection.MODNAME + "] You already set your Hypixel API key. Run " + EnumChatFormatting.DARK_GREEN + "/" + getCommandName() + " apikey check " + EnumChatFormatting.GREEN + "to see usage statistics.";
                color = EnumChatFormatting.GREEN;
                colorSecondary = EnumChatFormatting.DARK_GREEN;
            } else {
                firstSentence = "[" + Cowlection.MODNAME + "] You haven't set your Hypixel API key yet or the API key is invalid.";
                color = EnumChatFormatting.RED;
                colorSecondary = EnumChatFormatting.DARK_RED;
            }
            main.getChatHelper().sendMessage(color, firstSentence + " Use " + colorSecondary + "/api new" + color + " to request a new API key from Hypixel or use " + colorSecondary + "/" + this.getCommandName() + " apikey <key>" + color + " to manually set your existing API key.");
        } else {
            String key = args[1];
            if ("check".equalsIgnoreCase(key)) {
                if (StringUtils.isNotEmpty(CredentialStorage.moo)) {
                    ApiUtils.fetchApiKeyInfo(CredentialStorage.moo, hyApiKey -> {
                        if (hyApiKey != null && hyApiKey.isSuccess()) {
                            HyApiKey.Record apiKeyRecord = hyApiKey.getRecord();
                            if (apiKeyRecord != null) {
                                main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "[" + Cowlection.MODNAME + "] Your Hypixel API key was used to execute a total of " + EnumChatFormatting.DARK_GREEN + Utils.formatNumber(apiKeyRecord.getTotalQueries())
                                        + EnumChatFormatting.GREEN + " API requests. In the last minute, " + EnumChatFormatting.DARK_GREEN + apiKeyRecord.getQueriesInPastMin() + EnumChatFormatting.GREEN + " out of maximum " + EnumChatFormatting.DARK_GREEN + apiKeyRecord.getLimit() + EnumChatFormatting.GREEN + " allowed requests were made.");
                            } else {
                                main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "[" + Cowlection.MODNAME + "] Your Hypixel API key seems to be valid, but processing usage statistics failed.");
                            }
                        } else {
                            String cause = hyApiKey != null ? hyApiKey.getCause() : null;
                            Cowlection.getInstance().getChatHelper().sendMessage(EnumChatFormatting.RED, "[" + Cowlection.MODNAME + "] Failed to check API key usage statistics: " + (cause != null ? cause : "unknown cause :c"));
                        }
                    });
                } else {
                    throw new MooCommandException("You haven't set your Hypixel API key yet. Use " + EnumChatFormatting.DARK_RED + "/api new" + EnumChatFormatting.RED + " to request a new API key from Hypixel or use " + EnumChatFormatting.DARK_RED + "/" + this.getCommandName() + " apikey <key>" + EnumChatFormatting.RED + " to manually set your existing API key.");
                }
            } else if (Utils.isValidUuid(key)) {
                main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, "[" + Cowlection.MODNAME + "] Validating API key...");
                main.getMoo().setMooIfValid(key, true);
            } else {
                throw new SyntaxErrorException("[" + Cowlection.MODNAME + "] That doesn't look like a valid API key... Did you want check your API key usage statistics? Run /" + getCommandName() + " apikey check");
            }
        }
    }

    private void handleWorldAge(String[] args) {
        if (args.length == 2) {
            boolean enable;
            switch (args[1]) {
                case "on":
                case "enable":
                    enable = true;
                    break;
                case "off":
                case "disable":
                    enable = false;
                    break;
                default:
                    main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Command usage: /" + getCommandName() + " worldage [on|off]");
                    return;
            }
            MooConfig.notifyServerAge = enable;
            main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "✔ " + (enable ? EnumChatFormatting.DARK_GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled") + EnumChatFormatting.GREEN + " world age notifications.");
            main.getConfig().syncFromFields();
        } else {
            long worldTime = Minecraft.getMinecraft().theWorld.getWorldTime();
            new TickDelay(() -> {
                WorldClient world = Minecraft.getMinecraft().theWorld;
                if (world == null) {
                    return;
                }
                String msgPrefix;
                long worldTime2 = world.getWorldTime();
                if (worldTime > worldTime2 || (worldTime2 - worldTime) < 15) {
                    // time is frozen
                    worldTime2 = world.getTotalWorldTime();
                    msgPrefix = "World time seems to be frozen at around " + worldTime + " ticks. ";
                    if (worldTime2 > 24 * 60 * 60 * 20) {
                        // total world time >24h
                        main.getChatHelper().sendMessage(EnumChatFormatting.GOLD, msgPrefix + "However, how long this world is loaded cannot be determined.");
                        return;
                    }
                    msgPrefix += "However, this world is probably";
                } else {
                    msgPrefix = "This world is";
                }
                long days = worldTime2 / 24000L + 1;
                long minutes = days * 20;
                long hours = minutes / 60;
                minutes -= hours * 60;

                main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, msgPrefix + " loaded around " + EnumChatFormatting.GOLD + days + " ingame days "
                        + EnumChatFormatting.YELLOW + "(= less than" + EnumChatFormatting.GOLD + (hours > 0 ? " " + hours + " hours" : "") + (minutes > 0 ? " " + minutes + " mins" : "") + ")");
            }, 20);
        }
    }
    //endregion

    //region sub-commands: update mod
    private void handleUpdate(String[] args) throws MooCommandException {
        if (args.length == 2 && args[1].equalsIgnoreCase("help")) {
            handleUpdateHelp();
            return;
        }
        boolean updateCheckStarted = main.getVersionChecker().runUpdateCheck(true);

        if (updateCheckStarted) {
            main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "➜ Checking for a newer mod version...");
            // VersionChecker#handleVersionStatus will run with a 5 seconds delay
        } else {
            long nextUpdate = main.getVersionChecker().getNextCheck();
            String waitingTime = String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(nextUpdate),
                    TimeUnit.MILLISECONDS.toSeconds(nextUpdate) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(nextUpdate)));
            throw new MooCommandException("⚠ Update checker is on cooldown. Please wait " + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + waitingTime + EnumChatFormatting.RESET + EnumChatFormatting.RED + " more minutes before checking again.");
        }
    }

    private void handleUpdateHelp() {
        main.getChatHelper().sendMessage(new ChatComponentText("➜ Update instructions:").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(true))
                .appendSibling(new ChatComponentText("\n➊" + EnumChatFormatting.YELLOW + " download latest mod version").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false)
                        .setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, main.getVersionChecker().getDownloadUrl()))
                        .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Download the latest version of " + Cowlection.MODNAME + "\n➜ Click to download latest mod file")))))
                .appendSibling(new ChatComponentText("\n➋" + EnumChatFormatting.YELLOW + " exit Minecraft").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false)
                        .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.GOLD + "➋" + EnumChatFormatting.YELLOW + " Without closing Minecraft first,\n" + EnumChatFormatting.YELLOW + "you can't delete the old .jar file!")))))
                .appendSibling(new ChatComponentText("\n➌" + EnumChatFormatting.YELLOW + " copy " + EnumChatFormatting.GOLD + Cowlection.MODNAME.replace(" ", "") + "-" + main.getVersionChecker().getNewVersion() + ".jar" + EnumChatFormatting.YELLOW + " into mods directory").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false)
                        .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/moo directory"))
                        .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Open mods directory with command " + EnumChatFormatting.GOLD + "/moo directory\n➜ Click to open mods directory")))))
                .appendSibling(new ChatComponentText("\n➍" + EnumChatFormatting.YELLOW + " delete old mod file " + EnumChatFormatting.GOLD + Cowlection.MODNAME.replace(" ", "") + "-" + Cowlection.VERSION + ".jar ").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false)))
                .appendSibling(new ChatComponentText("\n➎" + EnumChatFormatting.YELLOW + " start Minecraft again").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false))));
    }
    //endregion

    // other helper methods:
    private void displayGuiScreen(GuiScreen gui) {
        // delay by 1 tick, because the chat closing would close the new gui instantly as well.
        new TickDelay(() -> Minecraft.getMinecraft().displayGuiScreen(gui), 1);
    }

    private void sendCommandUsage(ICommandSender sender) {
        IChatComponent usage = new MooChatComponent("➜ " + Cowlection.MODNAME + " commands:").gold().bold()
                .appendSibling(createCmdHelpEntry("config", "Open mod's configuration"))
                .appendSibling(new MooChatComponent("\n").reset().white().appendText(EnumChatFormatting.DARK_GREEN + "  ❢" + EnumChatFormatting.LIGHT_PURPLE + EnumChatFormatting.ITALIC + " To move the Dungeons overlay: " + EnumChatFormatting.WHITE + "/" + getCommandName() + " config " + EnumChatFormatting.GRAY + "➡ " + EnumChatFormatting.WHITE + "SB Dungeons " + EnumChatFormatting.GRAY + "➡ " + EnumChatFormatting.WHITE + "Dungeon Performance Overlay"))
                .appendSibling(new MooChatComponent("\n").reset().gray().appendText(EnumChatFormatting.DARK_GREEN + "  ❢" + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + " Commands marked with §d§l⚷" + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + " require a valid API key"))
                .appendSibling(createCmdHelpSection(1, "Best friends, friends & other players"))
                .appendSibling(createCmdHelpEntry("stalk", "Get info of player's status §d§l⚷"))
                .appendSibling(createCmdHelpEntry("add", "Add best friends"))
                .appendSibling(createCmdHelpEntry("remove", "Remove best friends"))
                .appendSibling(createCmdHelpEntry("list", "View list of best friends"))
                .appendSibling(createCmdHelpEntry("online", "View list of best friends that are currently online §d§l⚷"))
                .appendSibling(createCmdHelpEntry("nameChangeCheck", "Force a scan for a changed name of a best friend (is done automatically as well)"))
                .appendSibling(createCmdHelpSection(2, "SkyBlock"))
                .appendSibling(createCmdHelpEntry("stalkskyblock", "Get info of player's SkyBlock stats §d§l⚷"))
                .appendSibling(createCmdHelpEntry("analyzeChests", "Analyze chests' contents and evaluate potential Bazaar value"))
                .appendSibling(createCmdHelpEntry("analyzeIsland", "Analyze a SkyBlock private island (inspect minions)"))
                .appendSibling(createCmdHelpEntry("waila", "Copy the 'thing' you're looking at (optional keybinding: Minecraft controls > Cowlection)"))
                .appendSibling(createCmdHelpEntry("dungeon", "SkyBlock Dungeons: display current dungeon performance"))
                .appendSibling(createCmdHelpEntry("dungeon party", "SkyBlock Dungeons: Shows armor and dungeon info about current party members " + EnumChatFormatting.GRAY + "(alias: " + EnumChatFormatting.WHITE + "/" + getCommandName() + " dp" + EnumChatFormatting.GRAY + ") §d§l⚷"))
                .appendSibling(createCmdHelpEntry("dungeon rules", "SkyBlock Dungeons: Edit rules for Party Finder " + EnumChatFormatting.GRAY + "(alias: " + EnumChatFormatting.WHITE + "/" + getCommandName() + " dr" + EnumChatFormatting.GRAY + ")"))
                .appendSibling(createCmdHelpSection(3, "Miscellaneous"))
                .appendSibling(createCmdHelpEntry("search", "Open Minecraft log search"))
                .appendSibling(createCmdHelpEntry("worldage", "Check how long the current world is loaded"))
                .appendSibling(createCmdHelpEntry("guiScale", "Change GUI scale"))
                .appendSibling(createCmdHelpEntry("rr", "Alias for /r without auto-replacement to /msg"))
                .appendSibling(createCmdHelpEntry("shrug", "¯\\_(ツ)_/¯"))
                .appendSibling(createCmdHelpEntry("discord", "Need help? Join the Cowshed discord"))
                .appendSibling(createCmdHelpSection(4, "Update mod"))
                .appendSibling(createCmdHelpEntry("update", "Check for new mod updates"))
                .appendSibling(createCmdHelpEntry("updateHelp", "Show mod update instructions"))
                .appendSibling(createCmdHelpEntry("version", "View results of last mod update check"))
                .appendSibling(createCmdHelpEntry("directory", "Open Minecraft's mods directory"))
                .appendFreshSibling(new MooChatComponent("➡ /commandslist " + EnumChatFormatting.YELLOW + "to list all commands added by your installed mods.").lightPurple().setSuggestCommand("/commandslist"))
                .appendFreshSibling(new MooChatComponent("➜ Need help with " + EnumChatFormatting.GOLD + Cowlection.MODNAME + EnumChatFormatting.GREEN + "? Do you have any questions, suggestions or other feedback? " + EnumChatFormatting.GOLD + "Join the Cowshed discord!").green().setUrl(Cowlection.INVITE_URL));
        sender.addChatMessage(usage);
    }

    private IChatComponent createCmdHelpSection(int nr, String title) {
        String prefix = Character.toString((char) (0x2789 + nr));
        return new ChatComponentText("\n").appendSibling(new ChatComponentText(prefix + " " + title).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(true)));
    }

    private IChatComponent createCmdHelpEntry(String cmd, String usage) {
        String command = "/" + this.getCommandName() + " " + cmd;

        return new MooChatComponent("\n").reset().appendSibling(new MooChatComponent.KeyValueChatComponent(command, usage, " ➡ ").setSuggestCommand(command));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args,
                    /* main */ "help", "config",
                    /* Best friends, friends & other players */ "stalk", "add", "remove", "list", "online", "nameChangeCheck",
                    /* SkyBlock */ "stalkskyblock", "skyblockstalk", "chestAnalyzer", "analyzeChests", "analyzeIsland", "waila", "whatAmILookingAt", "dungeon",
                    /* miscellaneous */ "search", "worldage", "serverage", "guiscale", "rr", "shrug", "apikey", "discord",
                    /* update mod */ "update", "updateHelp", "version", "directory",
                    /* rarely used aliases */ "askPolitelyWhereTheyAre", "askPolitelyAboutTheirSkyBlockProgress", "year", "whatyearisit");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("waila") || args[0].equalsIgnoreCase("whatAmILookingAt"))) {
            return getListOfStringsMatchingLastWord(args, "all", "main");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return getListOfStringsMatchingLastWord(args, main.getFriendsHandler().getBestFriends());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("dungeon")) {
            return getListOfStringsMatchingLastWord(args, "party", "rules", "enter", "leave");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("worldage") || args[0].equalsIgnoreCase("serverage"))) {
            return getListOfStringsMatchingLastWord(args, "off", "on", "disable", "enable");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("chestAnalyzer") || args[0].equalsIgnoreCase("chestAnalyser") || args[0].equalsIgnoreCase("analyzeChests") || args[0].equalsIgnoreCase("analyseChests"))) {
            return getListOfStringsMatchingLastWord(args, "stop");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("apikey")) {
            return getListOfStringsMatchingLastWord(args, "check");
        }
        String commandArg = args[0].toLowerCase();
        if (args.length == 2 && (commandArg.equals("s") || commandArg.equals("ss") || commandArg.equals("namechangecheck") || commandArg.contains("stalk") || commandArg.contains("askpolitely"))) { // stalk & stalkskyblock + namechangecheck
            return getListOfStringsMatchingLastWord(args, main.getPlayerCache().getAllNamesSorted());
        }
        return null;
    }
}
