package de.cowtipper.cowlection.command;

import de.cowtipper.cowlection.Cowlection;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

import java.util.List;

/**
 * This is not a real command. Its sole purpose is to add tab completion for usernames to server-side commands that do not provide tab completion for usernames by default.
 */
public class TabCompletableCommand extends CommandBase {
    private final Cowlection main;
    private final String cmdName;

    public TabCompletableCommand(Cowlection main, String cmdName) {
        this.main = main;
        this.cmdName = cmdName;
    }

    @Override
    public String getCommandName() {
        return cmdName;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + cmdName + " [additional parameters]. This client-command provides username tab-completion for the equivalent server-side command.";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        String prefix;
        String arguments;
        if (args.length >= 1 && args[0].equalsIgnoreCase("say")) {
            // work-around so you can still say '[commandName]' in chat without triggering the server-side command
            prefix = "";
            arguments = CommandBase.buildString(args, 1);
        } else {
            // send client-command to server
            prefix = "/";
            arguments = CommandBase.buildString(args, 0);
        }
        Minecraft.getMinecraft().thePlayer.sendChatMessage(prefix + getCommandName() + (!arguments.isEmpty() ? " " + arguments : ""));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1 || args.length == 2) {
            // suggest recently 'seen' usernames as tab-completion options.
            return getListOfStringsMatchingLastWord(args, main.getPlayerCache().getAllNamesSorted());
        }
        return null;
    }
}
