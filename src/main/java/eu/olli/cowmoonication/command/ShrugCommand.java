package eu.olli.cowmoonication.command;

import eu.olli.cowmoonication.Cowmoonication;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

public class ShrugCommand extends CommandBase {
    private final Cowmoonication main;

    public ShrugCommand(Cowmoonication main) {
        this.main = main;
    }

    @Override
    public String getCommandName() {
        return "shrug";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/shrug [message]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        main.getChatHelper().sendShrug(args);
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
