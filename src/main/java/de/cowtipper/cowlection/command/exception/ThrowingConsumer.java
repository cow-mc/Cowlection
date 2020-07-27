package de.cowtipper.cowlection.command.exception;

import de.cowtipper.cowlection.Cowlection;
import net.minecraft.command.CommandException;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.util.function.Consumer;

@FunctionalInterface
public interface ThrowingConsumer<T> extends Consumer<T> {
    @Override
    default void accept(T t) {
        try {
            acceptThrows(t);
        } catch (CommandException e) {
            IChatComponent errorMsg = new ChatComponentTranslation(e.getMessage(), e.getErrorObjects());
            errorMsg.getChatStyle().setColor(EnumChatFormatting.RED);
            Cowlection.getInstance().getChatHelper().sendMessage(errorMsg);
        }
    }

    void acceptThrows(T t) throws CommandException;
}
