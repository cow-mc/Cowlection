package eu.olli.cowlection.command.exception;

import net.minecraft.command.CommandException;

public class MooCommandException extends CommandException {
    public MooCommandException(String msg) {
        super("cowlection.commands.generic.exception", msg);
    }
}
