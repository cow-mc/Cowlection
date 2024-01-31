package de.cowtipper.cowlection.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
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
        if (Minecraft.getMinecraft().thePlayer == null) {
            putOfflineMessage(chatComponent);
        } else {
            Minecraft.getMinecraft().thePlayer.addChatMessage(chatComponent);
        }
    }

    private void putOfflineMessage(IChatComponent chatComponent) {
        if (offlineMessages.isEmpty()) {
            // had no offline messages before
            MinecraftForge.EVENT_BUS.register(this);
        }
        offlineMessages.add(chatComponent);
    }

    @SubscribeEvent
    public void onPlayerWorldJoin(EntityJoinWorldEvent e) {
        if (e.entity == Minecraft.getMinecraft().thePlayer) {
            new TickDelay(this::sendOfflineMessages, 6 * 20);
        }
    }

    private void sendOfflineMessages() {
        if (Minecraft.getMinecraft().thePlayer != null) {
            if (offlineMessages.size() > 0) {
                Minecraft.getMinecraft().thePlayer.playSound("random.levelup", 0.4F, 0.8F);
            }

            for (IChatComponent offlineMessage : offlineMessages) {
                sendMessage(offlineMessage);
            }
            offlineMessages.clear();
            MinecraftForge.EVENT_BUS.unregister(this);
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
        String chatMsg = "¯\\_(ツ)_/¯";
        if (args.length > 0) {
            chatMsg = String.join(" ", args) + " " + chatMsg;
        }
        Minecraft.getMinecraft().thePlayer.sendChatMessage(chatMsg);
    }

    public void sendServerCommand(String command) {
        EntityPlayerSP thePlayer = Minecraft.getMinecraft().thePlayer;
        if (thePlayer != null) {
            thePlayer.sendChatMessage(command);
        }
    }
}
