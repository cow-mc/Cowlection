package eu.olli.cowmoonication.listener;

import eu.olli.cowmoonication.Cowmoonication;
import eu.olli.cowmoonication.config.MooConfig;
import eu.olli.cowmoonication.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
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
    private static final Pattern LOGIN_LOGOUT_NOTIFICATION = Pattern.compile("^(?<type>§aFriend|§2Guild) > §r(?<rank>§[0-9a-f])(?<playerName>[\\w]+)(?<joinLeave> §r§e(?:joined|left)\\.)§r$");
    private static final Pattern PRIVATE_MESSAGE_RECEIVED_PATTERN = Pattern.compile("^From (?:\\[.*?] )?(\\w+): ");
    private final Cowmoonication main;
    private String lastTypedChars = "";
    private String lastPMSender;

    public ChatListener(Cowmoonication main) {
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
                String joinLeave = notificationMatcher.group("joinLeave");

                if (MooConfig.showBestFriendNotifications) {
                    boolean isBestFriend = main.getFriends().isBestFriend(playerName, false);
                    if (isBestFriend) {
                        // replace default (friend/guild) notification with best friend notification
                        main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, "" + EnumChatFormatting.DARK_GREEN + EnumChatFormatting.BOLD + "Best friend" + EnumChatFormatting.DARK_GREEN + " > " + EnumChatFormatting.RESET + rank + playerName + joinLeave);
                        e.setCanceled(true);
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
                    MooConfig.moo = moo;
                    main.getConfig().syncFromFields();
                    main.getChatHelper().sendMessage(EnumChatFormatting.GREEN, "Added updated API key in Cowmoonication config!");
                }
            }
        }
    }

    @SubscribeEvent
    public void onClickOnChat(GuiScreenEvent.MouseInputEvent.Pre e) {
        if (e.gui instanceof GuiChat) {
            if (!Mouse.getEventButtonState() && Mouse.getEventButton() == 1 && Keyboard.isKeyDown(Keyboard.KEY_LMENU)) { // alt key pressed and right mouse button being released
                IChatComponent chatComponent = Minecraft.getMinecraft().ingameGUI.getChatGUI().getChatComponent(Mouse.getX(), Mouse.getY());
                if (chatComponent != null) {
                    boolean copyWithFormatting = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
                    String chatData;
                    if (copyWithFormatting) {
                        chatData = main.getChatHelper().cleanChatComponent(chatComponent);
                    } else {
                        chatData = StringUtils.stripControlCodes(chatComponent.getUnformattedText());
                    }
                    GuiControls.setClipboardString(chatData);
                    main.getChatHelper().sendAboveChatMessage(EnumChatFormatting.YELLOW + "Copied chat component to clipboard:", "" + EnumChatFormatting.BOLD + EnumChatFormatting.GOLD + "\u276E" + EnumChatFormatting.RESET + (copyWithFormatting ? chatComponent.getUnformattedText() : chatData) + EnumChatFormatting.BOLD + EnumChatFormatting.GOLD + "\u276F");
                }
            }
        }
    }

    @SubscribeEvent
    public void onReplyToMsg(GuiScreenEvent.KeyboardInputEvent.Pre e) {
        // TODO Switch to more reliable way: GuiTextField#writeText on GuiChat#inputField (protected field) via reflections [using "Open Command"-key isn't detected currently]
        if (lastPMSender != null && e.gui instanceof GuiChat && lastTypedChars.length() < 3 && Keyboard.getEventKeyState()) {
            char eventCharacter = Keyboard.getEventCharacter();
            if (!CharUtils.isAsciiControl(eventCharacter)) {
                lastTypedChars += eventCharacter;
                if (lastTypedChars.equalsIgnoreCase("/r ")) {
                    // replace /r with /msg <last user>
                    main.getChatHelper().sendAboveChatMessage("Sending message to " + lastPMSender + "!");
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

    @SubscribeEvent
    public void onPrivateMsgReceive(ClientChatReceivedEvent e) {
        if (e.type != 2) {
            Matcher matcher = PRIVATE_MESSAGE_RECEIVED_PATTERN.matcher(e.message.getUnformattedText());
            if (matcher.find()) {
                this.lastPMSender = matcher.group(1);
            }
        }
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
