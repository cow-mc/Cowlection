package eu.olli.cowmoonication.command;

import eu.olli.cowmoonication.Cowmoonication;
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
    private final Cowmoonication main;
    private final String cmdName;

    public TabCompletableCommand(Cowmoonication main, String cmdName) {
        this.main = main;
        this.cmdName = cmdName;
    }

    @Override
    public String getCommandName() {
        return cmdName;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        // send client-command to server
        Minecraft.getMinecraft().thePlayer.sendChatMessage("/" + getCommandName() + " " + CommandBase.buildString(args, 0));
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
