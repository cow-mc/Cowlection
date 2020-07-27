package de.cowtipper.cowlection.command.exception;

import net.minecraft.command.SyntaxErrorException;
import net.minecraft.util.EnumChatFormatting;

public class InvalidPlayerNameException extends SyntaxErrorException {
    public InvalidPlayerNameException(String playerName) {
        super(EnumChatFormatting.DARK_RED + playerName + EnumChatFormatting.RED + "? This... doesn't look like a valid username.");
    }
}
