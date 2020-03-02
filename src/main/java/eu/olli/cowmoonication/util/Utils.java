package eu.olli.cowmoonication.util;

import eu.olli.cowmoonication.Cowmoonication;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static final Pattern VALID_USERNAME = Pattern.compile("^[\\w]{1,16}$");
    private static final Pattern USELESS_JSON_CONTENT_PATTERN = Pattern.compile("\"[A-Za-z]+\":false,?");
    private final Cowmoonication main;
    private String[] aboveChatMessage;
    private long aboveChatMessageExpiration;

    public Utils(Cowmoonication main) {
        this.main = main;
    }

    public void sendMessage(String text) {
        sendMessage(new ChatComponentText(text));
    }

    public void sendMessage(IChatComponent chatComponent) {
        ClientChatReceivedEvent event = new ClientChatReceivedEvent((byte) 1, chatComponent);
        MinecraftForge.EVENT_BUS.post(event);
        if (!event.isCanceled()) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(event.message);
        }
    }

    public void sendAboveChatMessage(String... text) {
        aboveChatMessage = text;
        aboveChatMessageExpiration = Minecraft.getSystemTime() + 5000;
    }

    public String[] getAboveChatMessage() {
        if (aboveChatMessageExpiration < Minecraft.getSystemTime()) {
            // message expired
            aboveChatMessage = null;
        }
        return aboveChatMessage;
    }

    public boolean isValidMcName(String username) {
        return VALID_USERNAME.matcher(username).matches();
    }

    public String cleanChatComponent(IChatComponent chatComponent) {
        String component = IChatComponent.Serializer.componentToJson(chatComponent);
        Matcher jsonMatcher = USELESS_JSON_CONTENT_PATTERN.matcher(component);
        return jsonMatcher.replaceAll("");
    }
}
