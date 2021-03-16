package de.cowtipper.cowlection.util;

import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

public class MooChatComponent extends ChatComponentText {
    public MooChatComponent(String msg) {
        super(msg);
    }

    public MooChatComponent black() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.BLACK));
        return this;
    }

    public MooChatComponent darkBlue() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.DARK_BLUE));
        return this;
    }

    public MooChatComponent darkGreen() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.DARK_GREEN));
        return this;
    }

    public MooChatComponent darkAqua() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.DARK_AQUA));
        return this;
    }

    public MooChatComponent darkRed() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.DARK_RED));
        return this;
    }

    public MooChatComponent darkPurple() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.DARK_PURPLE));
        return this;
    }

    public MooChatComponent gold() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.GOLD));
        return this;
    }

    public MooChatComponent gray() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.GRAY));
        return this;
    }

    public MooChatComponent darkGray() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.DARK_GRAY));
        return this;
    }

    public MooChatComponent blue() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.BLUE));
        return this;
    }

    public MooChatComponent green() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.GREEN));
        return this;
    }

    public MooChatComponent aqua() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.AQUA));
        return this;
    }

    public MooChatComponent red() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.RED));
        return this;
    }

    public MooChatComponent lightPurple() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.LIGHT_PURPLE));
        return this;
    }

    public MooChatComponent yellow() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.YELLOW));
        return this;
    }

    public MooChatComponent white() {
        setChatStyle(getChatStyle().setColor(EnumChatFormatting.WHITE));
        return this;
    }

    public MooChatComponent obfuscated() {
        setChatStyle(getChatStyle().setObfuscated(true));
        return this;
    }

    public MooChatComponent bold() {
        setChatStyle(getChatStyle().setBold(true));
        return this;
    }

    public MooChatComponent strikethrough() {
        setChatStyle(getChatStyle().setStrikethrough(true));
        return this;
    }

    public MooChatComponent underline() {
        setChatStyle(getChatStyle().setUnderlined(true));
        return this;
    }

    public MooChatComponent italic() {
        setChatStyle(getChatStyle().setItalic(true));
        return this;
    }

    public MooChatComponent reset() {
        setChatStyle(getChatStyle().setParentStyle(null).setBold(false).setItalic(false).setObfuscated(false).setUnderlined(false).setStrikethrough(false));
        return this;
    }

    public MooChatComponent setHover(IChatComponent hover) {
        setChatStyle(getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
        return this;
    }

    public MooChatComponent setUrl(String url) {
        setUrl(url, new KeyValueTooltipComponent("Click to visit", url));
        return this;
    }

    public MooChatComponent setUrl(String url, String hover) {
        setUrl(url, new MooChatComponent(hover).yellow());
        return this;
    }

    public MooChatComponent setUrl(String url, IChatComponent hover) {
        setChatStyle(getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)));
        setHover(hover);
        return this;
    }

    public MooChatComponent setSuggestCommand(String command) {
        setSuggestCommand(command, true);
        return this;
    }

    public MooChatComponent setSuggestCommand(String command, boolean addTooltip) {
        setChatStyle(getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)));
        if (addTooltip) {
            setHover(new KeyValueChatComponent("Run", command, " "));
        }
        return this;
    }

    /**
     * Appends the given component in a new line, without inheriting formatting of previous siblings.
     *
     * @see ChatComponentText#appendSibling appendSibling
     */
    public MooChatComponent appendFreshSibling(IChatComponent sibling) {
        this.siblings.add(new ChatComponentText("\n").appendSibling(sibling));
        return this;
    }

    public static class KeyValueChatComponent extends MooChatComponent {
        public KeyValueChatComponent(String key, String value) {
            this(key, value, ": ");
        }

        public KeyValueChatComponent(String key, String value, String separator) {
            super(key);
            appendText(separator);
            gold().appendSibling(new MooChatComponent(value).yellow());
        }
    }

    public static class KeyValueTooltipComponent extends MooChatComponent {
        public KeyValueTooltipComponent(String key, String value) {
            super(key);
            appendText(": ");
            gray().appendSibling(new MooChatComponent(value).yellow());
        }
    }
}
