package de.cowtipper.cowlection.listener;

import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.config.CredentialStorage;
import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.data.Friend;
import de.cowtipper.cowlection.data.HySkyBlockStats;
import de.cowtipper.cowlection.util.ApiUtils;
import de.cowtipper.cowlection.util.MooChatComponent;
import de.cowtipper.cowlection.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.CharUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener {
    /**
     * Examples:
     * - §aFriend > §r§aNAME §r§eleft.§r
     * - §2Guild > §r§aNAME §r§eleft.§r
     */
    private static final Pattern LOGIN_LOGOUT_NOTIFICATION = Pattern.compile("^(?<type>§aFriend|§2Guild) > §r(?<rank>§[0-9a-f])(?<playerName>\\w+)(?<joinLeaveSuffix> §r§e(?<joinedLeft>joined|left)\\.)§r$");
    private static final Pattern CHAT_MESSAGE_RECEIVED_PATTERN = Pattern.compile("^(?:Party|Guild) > (?:\\[.*?] )?(\\w+)(?: \\[.*?])?: ");
    private static final Pattern PRIVATE_MESSAGE_RECEIVED_PATTERN = Pattern.compile("^From (?:\\[.*?] )?(\\w+): ");
    private static final Pattern PARTY_OR_GAME_INVITE_PATTERN = Pattern.compile("^-+\\s+(?:\\[.*?] )?(\\w+) has invited you ");
    private static final Pattern DUNGEON_FINDER_JOINED_PATTERN = Pattern.compile("^Party Finder > (\\w+) joined the dungeon group! \\(([A-Z][a-z]+) Level (\\d+)\\)$");
    private final Cowlection main;
    private String lastTypedChars = "";
    private String lastPMSender;

    public ChatListener(Cowlection main) {
        this.main = main;
    }

    @SubscribeEvent
    public void onLogInOutMessage(ClientChatReceivedEvent e) {
        if (e.type != 2) { // normal chat or system msg (not above action bar)
            String text = e.message.getUnformattedText();
            Matcher notificationMatcher = LOGIN_LOGOUT_NOTIFICATION.matcher(e.message.getFormattedText());

            if (MooConfig.doMonitorNotifications() && text.length() < 42 && notificationMatcher.matches()) {
                // we got a login or logout notification!
                main.getLogger().info(text);

                String type = notificationMatcher.group("type");
                String rank = notificationMatcher.group("rank");
                String playerName = notificationMatcher.group("playerName");
                String joinLeaveSuffix = notificationMatcher.group("joinLeaveSuffix");
                String joinedLeft = notificationMatcher.group("joinedLeft");

                boolean isBestFriend = main.getFriendsHandler().isBestFriend(playerName, false);
                if (isBestFriend) {
                    switch (joinedLeft) {
                        case "joined":
                            main.getPlayerCache().addBestFriend(playerName);
                            if (MooConfig.enableBestFriendNotificationSound && Minecraft.getMinecraft().thePlayer != null) {
                                Minecraft.getMinecraft().thePlayer.playSound("random.pop", Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MASTER), 1);
                            }
                            break;
                        case "left":
                            main.getPlayerCache().removeBestFriend(playerName);
                            break;
                        default:
                            // player neither left nor joined?!
                            return;
                    }
                    if (MooConfig.showBestFriendNotifications) {
                        // replace default (friend/guild) notification with best friend notification
                        e.message = new ChatComponentText("" + EnumChatFormatting.DARK_GREEN + EnumChatFormatting.BOLD + "Best friend" + EnumChatFormatting.DARK_GREEN + " > " + EnumChatFormatting.RESET + rank + playerName + joinLeaveSuffix);
                        return;
                    }
                }
                if (!MooConfig.showFriendNotifications && "§aFriend".equals(type)) {
                    e.setCanceled(true);
                } else if (!MooConfig.showGuildNotifications && "§2Guild".equals(type)) {
                    e.setCanceled(true);
                }
            } else if (text.length() == 56 && text.startsWith("Your new API key is ")) {
                // Your new API key is 00000000-0000-0000-0000-000000000000
                String moo = text.substring(20, 56);
                if (Utils.isValidUuid(moo)) {
                    main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, "[" + Cowlection.MODNAME + "] Verifying the new API key...");
                    main.getMoo().setMooIfValid(moo, true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onClickOnChat(GuiScreenEvent.MouseInputEvent.Pre e) {
        if (Mouse.getEventButton() < 0) {
            // no button press, just mouse-hover
            return;
        }
        if (e.gui instanceof GuiChat) {
            if (!Mouse.getEventButtonState() && Mouse.getEventButton() == 1 && Keyboard.isKeyDown(Keyboard.KEY_LMENU)) { // alt key pressed and right mouse button being released
                IChatComponent chatComponent = Minecraft.getMinecraft().ingameGUI.getChatGUI().getChatComponent(Mouse.getX(), Mouse.getY());
                if (chatComponent != null) {
                    boolean copyWithFormatting = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
                    String chatData;
                    if (copyWithFormatting) {
                        chatData = main.getChatHelper().cleanChatComponent(chatComponent);
                    } else {
                        chatData = EnumChatFormatting.getTextWithoutFormattingCodes(chatComponent.getUnformattedText());
                        if (chatData.startsWith(": ")) {
                            chatData = chatData.substring(2);
                        }
                    }
                    GuiControls.setClipboardString(chatData);
                    main.getChatHelper().sendAboveChatMessage(EnumChatFormatting.YELLOW + "Copied chat component to clipboard:", "" + EnumChatFormatting.BOLD + EnumChatFormatting.GOLD + "❮" + EnumChatFormatting.RESET + (copyWithFormatting ? chatComponent.getUnformattedText() : chatData) + EnumChatFormatting.BOLD + EnumChatFormatting.GOLD + "❯");
                }
            }
        }
    }

    @SubscribeEvent
    public void onReplyToMsg(GuiScreenEvent.KeyboardInputEvent.Pre e) {
        // TODO Switch to more reliable way: GuiTextField#writeText on GuiChat#inputField (protected field) via reflections [using "Open Command"-key isn't detected currently]
        if (MooConfig.fixReplyCmd && lastPMSender != null && e.gui instanceof GuiChat && lastTypedChars.length() < 3 && Keyboard.getEventKeyState()) {
            char eventCharacter = Keyboard.getEventCharacter();
            if (!CharUtils.isAsciiControl(eventCharacter)) {
                lastTypedChars += eventCharacter;
                if (lastTypedChars.equalsIgnoreCase("/r ")) {
                    // replace /r with /msg <last user>
                    main.getChatHelper().sendAboveChatMessage("Sending message to " + lastPMSender + "! "
                            + EnumChatFormatting.GRAY + "Alternatively use /rr");
                    Minecraft.getMinecraft().displayGuiScreen(new GuiChat("/w " + lastPMSender + " "));
                }
            } else if (Keyboard.getEventKey() == Keyboard.KEY_BACK) { // Backspace
                lastTypedChars = lastTypedChars.substring(0, Math.max(lastTypedChars.length() - 1, 0));
            }
        }
    }

    @SubscribeEvent
    public void onChatOpen(GuiOpenEvent e) {
        if (e.gui instanceof GuiChat) {
            lastTypedChars = "";
        }
    }

    // priority = highest to ignore other mods modifying the chat output
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onChatMsgReceive(ClientChatReceivedEvent e) {
        if (e.type != 2) {
            String messageSender = null;

            String message = EnumChatFormatting.getTextWithoutFormattingCodes(e.message.getUnformattedText());

            Matcher privateMessageMatcher = PRIVATE_MESSAGE_RECEIVED_PATTERN.matcher(message);
            Matcher chatMessageMatcher = CHAT_MESSAGE_RECEIVED_PATTERN.matcher(message);
            Matcher partyOrGameInviteMatcher = PARTY_OR_GAME_INVITE_PATTERN.matcher(message);
            Matcher dungeonPartyFinderJoinedMatcher = DUNGEON_FINDER_JOINED_PATTERN.matcher(message);
            if (privateMessageMatcher.find()) {
                messageSender = privateMessageMatcher.group(1);
                if (!"stash".equals(messageSender)) {
                    this.lastPMSender = messageSender;
                }
            } else if (chatMessageMatcher.find()) {
                messageSender = chatMessageMatcher.group(1);
            } else if (partyOrGameInviteMatcher.find()) {
                messageSender = partyOrGameInviteMatcher.group(1);
            } else if (dungeonPartyFinderJoinedMatcher.find()) {
                messageSender = dungeonPartyFinderJoinedMatcher.group(1);
                if (CredentialStorage.isMooValid) {
                    boolean joinedYourself = messageSender.equals(Minecraft.getMinecraft().thePlayer.getName());
                    if (!joinedYourself && MooConfig.getDungPartyFinderPlayerLookupDisplay() != MooConfig.Setting.DISABLED) {
                        // another player joined via Dungeon Party Finder
                        String dungeonClass = dungeonPartyFinderJoinedMatcher.group(2) + " Lvl " + dungeonPartyFinderJoinedMatcher.group(3);
                        getNewDungeonPartyMemberDetails(messageSender, dungeonClass);
                    } else if (joinedYourself && MooConfig.dungPartyFinderPartyLookup) {
                        // successfully joined another party via Dungeon Party Finder
                        main.getDungeonCache().lookupPartyMembers();
                    }
                }
            } else if (CredentialStorage.isMooValid && MooConfig.dungPartyFullLookup && message.equals("Party Finder > Your dungeon group is full! Click here to warp to the dungeon!")
                    && (Minecraft.getMinecraft().currentScreen == null || Minecraft.getMinecraft().currentScreen instanceof GuiChat)) {
                ClientCommandHandler.instance.executeCommand(Minecraft.getMinecraft().thePlayer, "/moo dp");
            }

            if (messageSender != null) {
                main.getPlayerCache().add(messageSender);
            }
        }
    }

    private void getNewDungeonPartyMemberDetails(String playerName, String dungeonClass) {
        ApiUtils.fetchFriendData(playerName, stalkedPlayer -> {
            if (stalkedPlayer != null && !stalkedPlayer.equals(Friend.FRIEND_NOT_FOUND)) {
                ApiUtils.fetchSkyBlockStats(stalkedPlayer, hySBStalking -> {
                    if (hySBStalking != null && hySBStalking.isSuccess()) {
                        HySkyBlockStats.Profile activeProfile = hySBStalking.getActiveProfile(stalkedPlayer.getUuid());
                        if (activeProfile == null) {
                            // player hasn't played SkyBlock but joined via dungeon party finder? Maybe an API error
                            return;
                        }
                        boolean outputAsChatMessages = MooConfig.getDungPartyFinderPlayerLookupDisplay() == MooConfig.Setting.TEXT;

                        HySkyBlockStats.Profile.Member member = activeProfile.getMember(stalkedPlayer.getUuid());
                        String gameModeIcon = activeProfile.getGameModeIcon();
                        String armorLookupPrefix = " ➲ " + gameModeIcon + (gameModeIcon.isEmpty() ? "" : " ") + EnumChatFormatting.DARK_GREEN + playerName;
                        String delimiter = "\n" + (outputAsChatMessages ? "    " : "");

                        HySkyBlockStats.Profile.Dungeons dungeons = member.getDungeons();
                        String dungeonTypesLevels = dungeons != null ? dungeons.getDungeonTypesLevels() : "";

                        String armorLookupResult = EnumChatFormatting.LIGHT_PURPLE + " ➜ " + EnumChatFormatting.GRAY + dungeonClass + dungeonTypesLevels + delimiter + String.join(delimiter, member.getArmor());

                        // active pet:
                        HySkyBlockStats.Profile.Pet activePet = member.getActivePet();
                        String petInfo = (outputAsChatMessages ? "\n  " : "\n\n") + EnumChatFormatting.GRAY + "Active pet: " + (activePet != null ? activePet.toFancyString() : "" + EnumChatFormatting.DARK_GRAY + EnumChatFormatting.ITALIC + "none");

                        // spirit pet:
                        HySkyBlockStats.Profile.Pet spiritPet = member.getPet("SPIRIT");
                        if (spiritPet != null) {
                            petInfo += EnumChatFormatting.GRAY + " (" + spiritPet.toFancyString() + EnumChatFormatting.GRAY + ")";
                        }

                        String highestFloorCompletions = "\n" + (outputAsChatMessages ? "  " : "") + EnumChatFormatting.GRAY + "Completed no dungeons yet";

                        String skyBlockDetails;
                        boolean hasPlayedDungeons = dungeons != null && dungeons.hasPlayed();
                        int totalDungeonCompletions = hasPlayedDungeons ? dungeons.getTotalDungeonCompletions() : 0;
                        if (outputAsChatMessages) {
                            // highest floor completions:
                            if (hasPlayedDungeons) {
                                highestFloorCompletions = dungeons.getHighestFloorCompletions(1, true).toString();
                            }
                            skyBlockDetails = armorLookupPrefix + armorLookupResult + petInfo + highestFloorCompletions;
                        } else {
                            // as a tooltip: == MooConfig.Setting.TOOLTIP
                            if (hasPlayedDungeons) {
                                // highest floor completions:
                                highestFloorCompletions = dungeons.getHighestFloorCompletions(3, false).toString();
                            }
                            skyBlockDetails = gameModeIcon + (gameModeIcon.isEmpty() ? "" : " " + EnumChatFormatting.WHITE) + EnumChatFormatting.BOLD + playerName + armorLookupResult + petInfo + highestFloorCompletions;
                        }

                        ApiUtils.fetchHyPlayerDetails(stalkedPlayer, hyPlayerData -> {
                            String foundDungeonsSecrets = "";
                            if (hyPlayerData != null) {
                                int foundSecrets = hyPlayerData.getAchievement("skyblock_treasure_hunter");
                                foundDungeonsSecrets = "\n" + (outputAsChatMessages ? "  " : "") + EnumChatFormatting.GRAY + "Found secrets: " + EnumChatFormatting.GOLD + Utils.formatNumber(foundSecrets);

                                String averageSecretsPerCompletion = null;
                                if (foundSecrets > 0 && totalDungeonCompletions > 0) {
                                    averageSecretsPerCompletion = Utils.formatDecimal(foundSecrets / (1d * totalDungeonCompletions));
                                }
                                if (averageSecretsPerCompletion != null) {
                                    foundDungeonsSecrets += EnumChatFormatting.GRAY + " (" + EnumChatFormatting.YELLOW + averageSecretsPerCompletion + EnumChatFormatting.GRAY + "/completion)";
                                }
                            }

                            MooChatComponent armorLookupComponent;
                            if (outputAsChatMessages) {
                                armorLookupComponent = new MooChatComponent(skyBlockDetails + foundDungeonsSecrets).green();
                            } else {
                                armorLookupComponent = new MooChatComponent(armorLookupPrefix + EnumChatFormatting.GREEN + (playerName.endsWith("s") ? "'" : "'s") + " dungeons info (hover me)").green()
                                        .setHover(new MooChatComponent(skyBlockDetails + foundDungeonsSecrets));
                            }
                            main.getChatHelper().sendMessage(armorLookupComponent.setSuggestCommand("/p kick " + playerName, outputAsChatMessages));
                        });
                    }
                });
            }
        });
    }

    @SubscribeEvent
    public void onRenderChatGui(RenderGameOverlayEvent.Chat e) {
        if (e.type == RenderGameOverlayEvent.ElementType.CHAT) {
            // render message above chat box
            String[] aboveChatMessage = main.getChatHelper().getAboveChatMessage();
            if (aboveChatMessage != null) {
                float chatHeightFocused = Minecraft.getMinecraft().gameSettings.chatHeightFocused;
                float chatScale = Minecraft.getMinecraft().gameSettings.chatScale;
                int chatBoxHeight = (int) (GuiNewChat.calculateChatboxHeight(chatHeightFocused) * chatScale);

                int defaultTextY = e.resolution.getScaledHeight() - chatBoxHeight - 30;

                for (int i = 0; i < aboveChatMessage.length; i++) {
                    String msg = aboveChatMessage[i];
                    int textY = defaultTextY - (aboveChatMessage.length - i) * (Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT + 1);
                    Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(msg, 2, textY, 0xffffff);
                }
            }
        }
    }
}
