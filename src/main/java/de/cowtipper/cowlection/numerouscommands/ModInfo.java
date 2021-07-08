package de.cowtipper.cowlection.numerouscommands;

import de.cowtipper.cowlection.command.NumerousCommandsCommand;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ModMetadata;

import java.util.*;

public class ModInfo {
    private final String name;
    private final ModMetadata metadata;
    private final Set<String> ownedPackages;
    private final Map<String, CommandInfo> commands;
    private final boolean hasUpdate;

    public ModInfo(ModContainer mod) {
        name = mod.getName();
        metadata = mod.getMetadata();
        ownedPackages = new HashSet<>(mod.getOwnedPackages());
        commands = new TreeMap<>();
        hasUpdate = ForgeVersion.getResult(mod).status == ForgeVersion.Status.OUTDATED;
    }

    public ModInfo() {
        name = "Unknown mod";
        metadata = new ModMetadata();
        metadata.modId = "unknownmodwithoutanid";
        metadata.name = name;
        metadata.authorList = Collections.singletonList(EnumChatFormatting.ITALIC + "Unknown");
        ownedPackages = new HashSet<>();
        commands = new TreeMap<>();
        hasUpdate = false;
    }

    public void addCommand(ICommand cmd, ICommandSender sender) {
        CommandInfo commandInfo = new CommandInfo(cmd, sender);
        if (cmd instanceof NumerousCommandsCommand) {
            commandInfo.setIsListCommandsCommand();
        }
        commands.put(cmd.getClass().getSimpleName() + cmd.getCommandName(), commandInfo);
    }

    public String getName() {
        return name;
    }

    public ModMetadata getModMetadata() {
        return metadata;
    }

    public Collection<CommandInfo> getCommands() {
        return commands.values();
    }

    public int getCommandsCount() {
        return commands.size();
    }

    public boolean hasUpdate() {
        return hasUpdate;
    }

    public boolean isOwnedPackage(String packAge) {
        return ownedPackages.contains(packAge);
    }
}
