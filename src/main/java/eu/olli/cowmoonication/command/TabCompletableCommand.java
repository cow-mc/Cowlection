package eu.olli.cowmoonication.command;

import com.mojang.realmsclient.util.Pair;
import eu.olli.cowmoonication.Cowmoonication;
import eu.olli.cowmoonication.config.MooConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

public class TabCompletableCommand extends CommandBase {
    private final Cowmoonication main;

    public TabCompletableCommand(Cowmoonication main) {
        this.main = main;
    }

    @Override
    public String getCommandName() {
        return "tabcompletablecommand";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return EnumChatFormatting.YELLOW + "Commands where player names can be Tab-completed: " + EnumChatFormatting.GOLD + StringUtils.join(getCommandAliases(), EnumChatFormatting.YELLOW + ", " + EnumChatFormatting.GOLD)
                + EnumChatFormatting.YELLOW + ". Use " + EnumChatFormatting.GOLD + "/moo config " + EnumChatFormatting.YELLOW + " to edit the list of commands with tab-completable usernames.";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        Pair<String, String> lastCommand = getLastCommand();
        if (lastCommand == null) {
            main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Something went wrong trying to process this command.");
        } else if (lastCommand.first().equalsIgnoreCase(getCommandName())) {
            main.getChatHelper().sendMessage(EnumChatFormatting.YELLOW, getCommandUsage(sender));
        } else {
            Minecraft.getMinecraft().thePlayer.sendChatMessage(lastCommand.second());
        }
    }

    /**
     * Work-around to get last used command name or alias (by default it's impossible to detect the used alias)
     *
     * @return 1st: last command used by thePlayer, 2nd: (full) last message sent thePlayer; or null if no command was sent as the last message
     */
    private Pair<String, String> getLastCommand() {
        List<String> sentMessages = Minecraft.getMinecraft().ingameGUI.getChatGUI().getSentMessages();
        String lastMessage = sentMessages.get(sentMessages.size() - 1);
        if (lastMessage.startsWith("/")) {
            int endOfCommandName = lastMessage.indexOf(" ");
            return Pair.of(lastMessage.substring(1, endOfCommandName == -1 ? lastMessage.length() : endOfCommandName),
                    lastMessage);
        }
        return null;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public List<String> getCommandAliases() {
        // list of commands that require a player name as one their 1st or 2nd argument
        return Arrays.asList(MooConfig.tabCompletableNamesCommands);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1 || args.length == 2) {
            return getListOfStringsMatchingLastWord(args, main.getPlayerCache().getAllNamesSorted());
        }
        return null;
    }
}
