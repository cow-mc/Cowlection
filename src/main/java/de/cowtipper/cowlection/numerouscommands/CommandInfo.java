package de.cowtipper.cowlection.numerouscommands;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;

import java.util.List;

public class CommandInfo {
    private final String name;
    private final List<String> aliases;
    private final String usage;
    private boolean isListCommandsCommand;

    public CommandInfo(ICommand cmd, ICommandSender sender) {
        name = cmd.getCommandName();
        aliases = cmd.getCommandAliases();
        usage = cmd.getCommandUsage(sender);
        isListCommandsCommand = false;
    }

    public String getName() {
        return name;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public String getUsage() {
        return (usage != null && !usage.replace("/", "").equalsIgnoreCase(name)) ? usage : null;
    }

    public boolean isListCommandsCommand() {
        return isListCommandsCommand;
    }

    public void setIsListCommandsCommand() {
        isListCommandsCommand = true;
    }
}
