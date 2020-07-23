package eu.olli.cowlection.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatHelper {
    private static final Pattern USELESS_JSON_CONTENT_PATTERN = Pattern.compile("\"[A-Za-z]+\":false,?");
    private static final int DISPLAY_DURATION = 5000;
    private final List<IChatComponent> offlineMessages = new ArrayList<>();
    private String[] aboveChatMessage;
    private long aboveChatMessageExpiration;

    public ChatHelper() {
    }

    public void sendMessage(EnumChatFormatting color, String text) {
        sendMessage(new ChatComponentText(text).setChatStyle(new ChatStyle().setColor(color)));
    }

    public void sendMessage(IChatComponent chatComponent) {
        ClientChatReceivedEvent event = new ClientChatReceivedEvent((byte) 1, chatComponent);
        MinecraftForge.EVENT_BUS.post(event);
        if (!event.isCanceled()) {
            if (Minecraft.getMinecraft().thePlayer == null) {
                offlineMessages.add(event.message);
            } else {
                Minecraft.getMinecraft().thePlayer.addChatMessage(event.message);
            }
        }
    }

    public void sendOfflineMessages() {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Iterator<IChatComponent> offlineMessages = this.offlineMessages.iterator();
            if (offlineMessages.hasNext()) {
                Minecraft.getMinecraft().thePlayer.playSound("random.levelup", 0.4F, 0.8F);
            }
            while (offlineMessages.hasNext()) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(offlineMessages.next());
                offlineMessages.remove();
            }
        }
    }

    public void sendAboveChatMessage(String... text) {
        aboveChatMessage = text;
        aboveChatMessageExpiration = Minecraft.getSystemTime() + DISPLAY_DURATION;
    }

    public String[] getAboveChatMessage() {
        if (aboveChatMessageExpiration < Minecraft.getSystemTime()) {
            // message expired
            aboveChatMessage = null;
        }
        return aboveChatMessage;
    }

    public String cleanChatComponent(IChatComponent chatComponent) {
        String component = IChatComponent.Serializer.componentToJson(chatComponent);
        Matcher jsonMatcher = USELESS_JSON_CONTENT_PATTERN.matcher(component);
        return jsonMatcher.replaceAll("");
    }

    public void sendShrug(String... args) {
        String chatMsg = "\u00AF\\_(\u30C4)_/\u00AF"; // ¯\\_(ツ)_/¯"
        if (args.length > 0) {
            chatMsg = String.join(" ", args) + " " + chatMsg;
        }
        Minecraft.getMinecraft().thePlayer.sendChatMessage(chatMsg);
    }
}
