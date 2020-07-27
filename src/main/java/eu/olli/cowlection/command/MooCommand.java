package eu.olli.cowlection.command;

import com.mojang.realmsclient.util.Pair;
import eu.olli.cowlection.Cowlection;
import eu.olli.cowlection.command.exception.ApiContactException;
import eu.olli.cowlection.command.exception.InvalidPlayerNameException;
import eu.olli.cowlection.command.exception.MooCommandException;
import eu.olli.cowlection.config.MooConfig;
import eu.olli.cowlection.config.MooGuiConfig;
import eu.olli.cowlection.data.*;
import eu.olli.cowlection.data.HySkyBlockStats.Profile.Pet;
import eu.olli.cowlection.handler.DungeonCache;
import eu.olli.cowlection.search.GuiSearch;
import eu.olli.cowlection.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.command.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraftforge.common.util.Constants;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MooCommand extends CommandBase {
    private final Cowlection main;

    public MooCommand(Cowlection main) {
        this.main = main;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            main.getChatHelper().sendMessage(EnumChatFormatting.GOLD, "Tried to say " + EnumChatFormatting.YELLOW + getCommandName() + EnumChatFormatting.GOLD + "? Use " + EnumChatFormatting.YELLOW + getCommandName() + " say [optional text]" + EnumChatFormatting.GOLD + " instead.\n"
                    + "Tried to use the command " + EnumChatFormatting.YELLOW + "/" + getCommandName() + EnumChatFormatting.GOLD + "? Use " + EnumChatFormatting.YELLOW + "/" + getCommandName() + " help" + EnumChatFormatting.GOLD + " for a list of available commands");
            return;
        }
        // sub commands: friends & other players
        if (args[0].equalsIgnoreCase("say")) {
            // work-around so you can still say 'moo' in chat without triggering the client-side command
            String msg = CommandBase.buildString(args, 1);
            Minecraft.getMinecraft().thePlayer.sendChatMessage(getCommandName() + (!msg.isEmpty() ? " " + msg : ""));
        } else if (args[0].equalsIgnoreCase("stalk")
                || args[0].equalsIgnoreCase("s")
                || args[0].equalsIgnoreCase("askPolitelyWhereTheyAre")) {
            if (args.length != 2) {
                throw new WrongUsageException("/" + getCommandName() + " stalk <playerName>");
            } else if (!Utils.isValidMcName(args[1])) {
                throw new InvalidPlayerNameException(args[1]);
            } else {
                handleStalking(args[1]);
            }
        } else if (args[0].equalsIgnoreCase("stalkskyblock") || args[0].equalsIgnoreCase("skyblockstalk")
                || args[0].equalsIgnoreCase("ss")
                || args[0].equalsIgnoreCase("stalksb") || args[0].equalsIgnoreCase("sbstalk")
                || args[0].equalsIgnoreCase("askPolitelyAboutTheirSkyBlockProgress")) {
            if (args.length != 2) {
                throw new WrongUsageException("/" + getCommandName() + " skyblockstalk <playerName>");
            } else if (!Utils.isValidMcName(args[1])) {
                throw new InvalidPlayerNameException(args[1]);
            } else {
                handleStalkingSkyBlock(args[1]);
            }
        } else if (args[0].equalsIgnoreCase("analyzeIsland")) {
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
        } else if (args[0].equalsIgnoreCase("deaths")) {
            DungeonCache dungeonCache = main.getDungeonCache();
            if (dungeonCache.isInDungeon()) {
                dungeonCache.sendDeathCounts();
            } else {
                throw new MooCommandException(EnumChatFormatting.DARK_RED + "Looks like you're not in a dungeon...");
            }
        } else if (args[0].equalsIgnoreCase("add")) {
            handleBestFriendAdd(args);
        } else if (args[0].equalsIgnoreCase("remove")) {
            handleBestFriendRemove(args);
        } else if (args[0].equalsIgnoreCase("list")) {
            handleListBestFriends();
        } else if (args[0].equalsIgnoreCase("nameChangeCheck")) {
            handleNameChangeCheck(args);
        }
        // sub-commands: miscellaneous
        else if (args[0].equalsIgnoreCase("config") || args[0].equalsIgnoreCase("toggle")) {
            new TickDelay(() -> Minecraft.getMinecraft().displayGuiScreen(new MooGuiConfig(null)), 1); // delay by 1 tick, because the chat closing would close the new gui instantly as well.
        } else if (args[0].equalsIgnoreCase("search")) {
            new TickDelay(() -> Minecraft.getMinecraft().displayGuiScreen(new GuiSearch(main.getConfigDirectory())), 1); // delay by 1 tick, because the chat closing would close the new gui instantly as well.
        } else if (args[0].equalsIgnoreCase("guiscale")) {
            int currentGuiScale = (Minecraft.getMinecraft()).gameSettings.guiScale;
            if (args.length == 1) {
                main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "\u279C Current GUI scale: " + EnumChatFormatting.DARK_GREEN + currentGuiScale);
            } else {
                int scale = MathHelper.parseIntWithDefault(args[1], -1);
                if (scale == -1 || scale > 10) {
                    throw new NumberInvalidException(EnumChatFormatting.DARK_RED + args[1] + EnumChatFormatting.RED + " is an invalid GUI scale value. Valid values are integers below 10");
                }
                Minecraft.getMinecraft().gameSettings.guiScale = scale;
                main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "\u2714 New GUI scale: " + EnumChatFormatting.DARK_GREEN + scale + EnumChatFormatting.GREEN + " (previous: " + EnumChatFormatting.DARK_GREEN + currentGuiScale + EnumChatFormatting.GREEN + ")");
            }
        } else if (args[0].equalsIgnoreCase("rr")) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage("/r " + CommandBase.buildString(args, 1));
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
                throw new MooCommandException("\u26A0 Update checker is on cooldown. Please wait " + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + waitingTime + EnumChatFormatting.RESET + EnumChatFormatting.RED + " more minutes before checking again.");
            }
        } else if (args[0].equalsIgnoreCase("updateHelp")) {
            main.getChatHelper().sendMessage(new ChatComponentText("\u279C Update instructions:").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(true))
                    .appendSibling(new ChatComponentText("\n\u278A" + EnumChatFormatting.YELLOW + " download latest mod version").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false)
                            .setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, main.getVersionChecker().getDownloadUrl()))
                            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Download the latest version of " + Cowlection.MODNAME + "\n\u279C Click to download latest mod file")))))
                    .appendSibling(new ChatComponentText("\n\u278B" + EnumChatFormatting.YELLOW + " exit Minecraft").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false)
                            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.GOLD + "\u278B" + EnumChatFormatting.YELLOW + " Without closing Minecraft first,\n" + EnumChatFormatting.YELLOW + "you can't delete the old .jar file!")))))
                    .appendSibling(new ChatComponentText("\n\u278C" + EnumChatFormatting.YELLOW + " copy " + EnumChatFormatting.GOLD + Cowlection.MODNAME.replace(" ", "") + "-" + main.getVersionChecker().getNewVersion() + ".jar" + EnumChatFormatting.YELLOW + " into mods directory").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false)
                            .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/moo directory"))
                            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Open mods directory with command " + EnumChatFormatting.GOLD + "/moo directory\n\u279C Click to open mods directory")))))
                    .appendSibling(new ChatComponentText("\n\u278D" + EnumChatFormatting.YELLOW + " delete old mod file " + EnumChatFormatting.GOLD + Cowlection.MODNAME.replace(" ", "") + "-" + Cowlection.VERSION + ".jar ").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false)))
                    .appendSibling(new ChatComponentText("\n\u278E" + EnumChatFormatting.YELLOW + " start Minecraft again").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false))));
        } else if (args[0].equalsIgnoreCase("version")) {
            main.getVersionChecker().handleVersionStatus(true);
        } else if (args[0].equalsIgnoreCase("directory") || args[0].equalsIgnoreCase("folder")) {
            try {
                Desktop.getDesktop().open(main.getModsDirectory());
            } catch (IOException e) {
                e.printStackTrace();
                throw new MooCommandException("\u2716 An error occurred trying to open the mod's directory. I guess you have to open it manually \u00af\\_(\u30c4)_/\u00af");
            }
        } else if (args[0].equalsIgnoreCase("help")) {
            sendCommandUsage(sender);
        }
        // "catch-all" remaining sub-commands
        else {
            main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Command " + EnumChatFormatting.DARK_RED + "/" + getCommandName() + " " + args[0] + EnumChatFormatting.RED + " doesn't exist. Use " + EnumChatFormatting.DARK_RED + "/" + getCommandName() + " help " + EnumChatFormatting.RED + "to show command usage.");
        }
    }

    private void handleApiKey(String[] args) throws CommandException {
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
                throw new SyntaxErrorException("That doesn't look like a valid API key...");
            }
        }
    }

    private void handleStalking(String playerName) throws CommandException {
        if (!Utils.isValidUuid(MooConfig.moo)) {
            throw new MooCommandException("You haven't set your Hypixel API key yet. Use " + EnumChatFormatting.DARK_RED + "/api new" + EnumChatFormatting.RED + " to request a new API key from Hypixel or use " + EnumChatFormatting.DARK_RED + "/" + this.getCommandName() + " apikey <key>" + EnumChatFormatting.RED + " to manually set your existing API key.");
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

    private void stalkPlayer(Friend stalkedPlayer) {
        ApiUtils.fetchPlayerStatus(stalkedPlayer, hyStalking -> {
            if (hyStalking != null && hyStalking.isSuccess()) {
                HyStalkingData.HySession session = hyStalking.getSession();
                if (session.isOnline()) {
                    main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD + stalkedPlayer.getName() + EnumChatFormatting.YELLOW + " is currently playing " + EnumChatFormatting.GOLD + session.getGameType() + EnumChatFormatting.YELLOW
                            + (session.getMode() != null ? ": " + EnumChatFormatting.GOLD + session.getMode() : "")
                            + (session.getMap() != null ? EnumChatFormatting.YELLOW + " (Map: " + EnumChatFormatting.GOLD + session.getMap() + EnumChatFormatting.YELLOW + ")" : ""));
                } else {
                    ApiUtils.fetchPlayerOfflineStatus(stalkedPlayer, hyPlayerData -> {
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

    private void handleStalkingSkyBlock(String playerName) throws CommandException {
        if (!Utils.isValidUuid(MooConfig.moo)) {
            throw new MooCommandException("You haven't set your Hypixel API key yet. Use " + EnumChatFormatting.DARK_RED + "/api new" + EnumChatFormatting.RED + " to request a new API key from Hypixel or use " + EnumChatFormatting.DARK_RED + "/" + this.getCommandName() + " apikey <key>" + EnumChatFormatting.RED + " to manually set your existing API key.");
        }
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
                String coinsBankAndPurse = (activeProfile.getCoinBank() >= 0) ? Utils.formatNumberWithAbbreviations(activeProfile.getCoinBank() + member.getCoinPurse()) : "API access disabled";
                Pair<String, String> fancyFirstJoined = member.getFancyFirstJoined();

                MooChatComponent wealthHover = new MooChatComponent("Accessible coins:").gold()
                        .appendFreshSibling(new MooChatComponent.KeyValueTooltipComponent("Purse", Utils.formatNumberWithAbbreviations(member.getCoinPurse())))
                        .appendFreshSibling(new MooChatComponent.KeyValueTooltipComponent("Bank", (activeProfile.getCoinBank() != -1) ? Utils.formatNumberWithAbbreviations(activeProfile.getCoinBank()) : "API access disabled"));
                if (activeProfile.coopCount() > 0) {
                    wealthHover.appendFreshSibling(new ChatComponentText(" "));
                    wealthHover.appendFreshSibling(new MooChatComponent.KeyValueTooltipComponent("Co-op members", String.valueOf(activeProfile.coopCount())));
                    wealthHover.appendFreshSibling(new MooChatComponent.KeyValueTooltipComponent("Co-ops' purses sum", Utils.formatNumberWithAbbreviations(activeProfile.getCoopCoinPurses(stalkedPlayer.getUuid()))));
                }

                MooChatComponent sbStats = new MooChatComponent("SkyBlock stats of " + stalkedPlayer.getName() + " (" + activeProfile.getCuteName() + ")").gold().bold().setUrl("https://sky.lea.moe/stats/" + stalkedPlayer.getName() + "/" + activeProfile.getCuteName(), "Click to view SkyBlock stats on sky.lea.moe")
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
                sbStats.appendFreshSibling(new MooChatComponent.KeyValueChatComponent("Profile age", fancyFirstJoined.first()).setHover(new MooChatComponent.KeyValueTooltipComponent("Join date", fancyFirstJoined.second())));

                main.getChatHelper().sendMessage(sbStats);
            } else {
                String cause = (hySBStalking != null) ? hySBStalking.getCause() : null;
                throw new ApiContactException("Hypixel", "couldn't stalk " + EnumChatFormatting.DARK_RED + stalkedPlayer.getName() + EnumChatFormatting.RED + (cause != null ? " (Reason: " + EnumChatFormatting.DARK_RED + cause + EnumChatFormatting.RED + ")" : "") + ".");
            }
        });
    }

    private void handleBestFriendAdd(String[] args) throws CommandException {
        if (args.length != 2) {
            throw new WrongUsageException("/" + getCommandName() + " add <playerName>");
        } else if (!Utils.isValidMcName(args[1])) {
            throw new InvalidPlayerNameException(args[1]);
        } else if (main.getFriendsHandler().isBestFriend(args[1], true)) {
            throw new MooCommandException(EnumChatFormatting.DARK_RED + args[1] + EnumChatFormatting.RED + " is a best friend already.");
        } else {
            // TODO Add check if 'best friend' is on normal friend list
            main.getChatHelper().sendMessage(EnumChatFormatting.GOLD, "Fetching " + EnumChatFormatting.YELLOW + args[1] + EnumChatFormatting.GOLD + "'s unique user id. This may take a few seconds...");
            // add friend async
            main.getFriendsHandler().addBestFriend(args[1]);
        }
    }

    private void handleBestFriendRemove(String[] args) throws CommandException {
        if (args.length != 2) {
            throw new WrongUsageException("/" + getCommandName() + " remove <playerName>");
        } else if (!Utils.isValidMcName(args[1])) {
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
        main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "\u279C Best friends: " + ((bestFriends.isEmpty())
                ? EnumChatFormatting.ITALIC + "none :c"
                : EnumChatFormatting.DARK_GREEN + String.join(EnumChatFormatting.GREEN + ", " + EnumChatFormatting.DARK_GREEN, bestFriends)));
    }

    private void handleNameChangeCheck(String[] args) throws CommandException {
        if (args.length != 2) {
            throw new WrongUsageException("/" + getCommandName() + " nameChangeCheck <playerName>");
        } else if (!Utils.isValidMcName(args[1])) {
            throw new InvalidPlayerNameException(args[1]);
        }
        Friend bestFriend = main.getFriendsHandler().getBestFriend(args[1]);
        if (bestFriend.equals(Friend.FRIEND_NOT_FOUND)) {
            throw new MooCommandException(EnumChatFormatting.DARK_RED + args[1] + EnumChatFormatting.RED + " isn't a best friend.");
        } else {
            main.getChatHelper().sendMessage(EnumChatFormatting.GOLD, "Checking if " + bestFriend.getName() + " changed their name... This will take a few seconds...");
            // check for name change async
            main.getFriendsHandler().updateBestFriend(bestFriend, true);
        }
    }

    @Override
    public String getCommandName() {
        return "moo";
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.singletonList("m");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + getCommandName() + " help";
    }

    private void sendCommandUsage(ICommandSender sender) {
        IChatComponent usage = new MooChatComponent("\u279C " + Cowlection.MODNAME + " commands:").gold().bold()
                .appendSibling(createCmdHelpSection(1, "Friends & other players"))
                .appendSibling(createCmdHelpEntry("stalk", "Get info of player's status"))
                .appendSibling(createCmdHelpEntry("stalkskyblock", "Get info of player's SkyBlock stats"))
                .appendSibling(createCmdHelpEntry("analyzeIsland", "Analyze a SkyBlock private island"))
                .appendSibling(createCmdHelpEntry("deaths", "SkyBlock Dungeons: death counts"))
                .appendSibling(createCmdHelpEntry("add", "Add best friends"))
                .appendSibling(createCmdHelpEntry("remove", "Remove best friends"))
                .appendSibling(createCmdHelpEntry("list", "View list of best friends"))
                .appendSibling(createCmdHelpEntry("nameChangeCheck", "Force a scan for a changed name of a best friend"))
                .appendSibling(createCmdHelpEntry("toggle", "Toggle join/leave notifications"))
                .appendSibling(createCmdHelpSection(2, "Miscellaneous"))
                .appendSibling(createCmdHelpEntry("config", "Open mod's configuration"))
                .appendSibling(createCmdHelpEntry("search", "Open Minecraft log search"))
                .appendSibling(createCmdHelpEntry("guiScale", "Change GUI scale"))
                .appendSibling(createCmdHelpEntry("rr", "Alias for /r without auto-replacement to /msg"))
                .appendSibling(createCmdHelpEntry("shrug", "\u00AF\\_(\u30C4)_/\u00AF")) // ¯\_(ツ)_/¯
                .appendSibling(createCmdHelpSection(3, "Update mod"))
                .appendSibling(createCmdHelpEntry("update", "Check for new mod updates"))
                .appendSibling(createCmdHelpEntry("updateHelp", "Show mod update instructions"))
                .appendSibling(createCmdHelpEntry("version", "View results of last mod update check"))
                .appendSibling(createCmdHelpEntry("directory", "Open Minecraft's mods directory"));
        sender.addChatMessage(usage);
    }

    private IChatComponent createCmdHelpSection(int nr, String title) {
        String prefix = Character.toString((char) (0x2789 + nr));
        return new ChatComponentText("\n").appendSibling(new ChatComponentText(prefix + " " + title).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(true)));
    }

    private IChatComponent createCmdHelpEntry(String cmd, String usage) {
        String command = "/" + this.getCommandName() + " " + cmd;

        return new MooChatComponent("\n").reset().appendSibling(new MooChatComponent.KeyValueChatComponent(command, usage, " \u27A1 ").setSuggestCommand(command));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args,
                    /* friends & other players */ "stalk", "askPolitelyWhereTheyAre", "stalkskyblock", "skyblockstalk", "askPolitelyAboutTheirSkyBlockProgress", "analyzeIsland", "deaths", "add", "remove", "list", "nameChangeCheck", "toggle",
                    /* miscellaneous */ "config", "search", "guiscale", "rr", "shrug", "apikey",
                    /* update mod */ "update", "updateHelp", "version", "directory",
                    /* help */ "help");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return getListOfStringsMatchingLastWord(args, main.getFriendsHandler().getBestFriends());
        } else if (args.length == 2 && args[0].toLowerCase().contains("stalk")) { // stalk & stalkskyblock
            return getListOfStringsMatchingLastWord(args, main.getPlayerCache().getAllNamesSorted());
        }
        return null;
    }
}
