package de.cowtipper.cowlection.command.exception;

import net.minecraft.command.CommandException;
import net.minecraft.util.EnumChatFormatting;

public class MooCommandException extends CommandException {
    private final String msg;

    public MooCommandException(String msg) {
        super("cowlection.commands.generic.exception", msg);
        this.msg = msg;
    }

    @Override
    public String getLocalizedMessage() {
        return EnumChatFormatting.getTextWithoutFormattingCodes(this.msg);
    }
}
