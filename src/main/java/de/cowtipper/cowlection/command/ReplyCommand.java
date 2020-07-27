package de.cowtipper.cowlection.command;

import de.cowtipper.cowlection.Cowlection;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

public class ReplyCommand extends CommandBase {
    private final Cowlection main;

    public ReplyCommand(Cowlection main) {
        this.main = main;
    }

    @Override
    public String getCommandName() {
        return "rr";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/rr <message>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        Minecraft.getMinecraft().thePlayer.sendChatMessage("/r " + CommandBase.buildString(args, 0));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
