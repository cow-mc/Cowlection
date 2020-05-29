package eu.olli.cowmoonication.command.exception;

import net.minecraft.command.CommandException;

public class MooCommandException extends CommandException {
    public MooCommandException(String msg) {
        super("cowmoonication.commands.generic.exception", msg);
    }
}
