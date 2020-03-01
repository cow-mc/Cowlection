package eu.olli.cowmoonication.listener;

import eu.olli.cowmoonication.Cowmoonication;
import eu.olli.cowmoonication.config.MooConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.CharUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener {
    private static final Pattern PRIVATE_MESSAGE_RECEIVED_PATTERN = Pattern.compile("^From (?:\\[.*?] )?(\\w+): ");
    private final Cowmoonication main;
    private String lastTypedChars = "";
    private String lastPMSender;

    public ChatListener(Cowmoonication main) {
        this.main = main;
    }

    @SubscribeEvent
    public void onLogInOutMessage(ClientChatReceivedEvent e) {
        if (e.type != 2 && MooConfig.filterFriendNotifications) { // normal chat or system msg
            String text = e.message.getUnformattedText();
            if (text.endsWith(" joined.") || text.endsWith(" left.") // Hypixel
                    || text.endsWith(" joined the game") || text.endsWith(" left the game.")) { // Spigot
                // TODO maybe check which server thePlayer is on and check for logout pattern accordingly
                int nameEnd = text.indexOf(" joined");
                if (nameEnd == -1) {
                    nameEnd = text.indexOf(" left");
                }
                boolean isBestFriend = main.getFriends().isBestFriend(text.substring(0, nameEnd));
                if (!isBestFriend) {
                    e.setCanceled(true);
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
                    String chatData = main.getUtils().cleanChatComponent(chatComponent);
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(new StringSelection(chatData), null);
                    main.getUtils().sendAboveChatMessage(EnumChatFormatting.YELLOW + "Copied chat component to clipboard:", "" + EnumChatFormatting.BOLD + EnumChatFormatting.GOLD + "\u276E" + EnumChatFormatting.RESET + chatComponent.getUnformattedText() + EnumChatFormatting.BOLD + EnumChatFormatting.GOLD + "\u276F");
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
                    main.getUtils().sendAboveChatMessage("Sending message to " + lastPMSender + "!");
                    Minecraft.getMinecraft().displayGuiScreen(new GuiChat("/msg " + lastPMSender + " "));
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
            String[] aboveChatMessage = main.getUtils().getAboveChatMessage();
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
