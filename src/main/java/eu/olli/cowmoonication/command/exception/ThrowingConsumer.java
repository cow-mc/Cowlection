package eu.olli.cowmoonication.command.exception;

import eu.olli.cowmoonication.Cowmoonication;
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
            Cowmoonication.getInstance().getChatHelper().sendMessage(errorMsg);
        }
    }

    void acceptThrows(T t) throws CommandException;
}
