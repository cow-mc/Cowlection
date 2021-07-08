package de.cowtipper.cowlection.command;

import de.cowtipper.cowlection.numerouscommands.ModInfo;
import de.cowtipper.cowlection.numerouscommands.NumerousCommandsGui;
import de.cowtipper.cowlection.util.TickDelay;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.util.*;

public class NumerousCommandsCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "commandslist";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("clientcommands", "listcommands");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + getCommandName();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        HashSet<String> ignoredMods = new HashSet<>(Arrays.asList("Minecraft Coder Pack", "Forge Mod Loader", "Minecraft Forge"));

        Map<String, ModInfo> modsInfo = new TreeMap<>();

        for (ModContainer mod : Loader.instance().getActiveModList()) {
            String modName = mod.getName();
            if (ignoredMods.contains(modName)) {
                // ignored mod
                continue;
            }
            modsInfo.put(modName, new ModInfo(mod));
        }

        ModInfo unknownMod = new ModInfo();
        for (Map.Entry<String, ICommand> cmdEntry : ClientCommandHandler.instance.getCommands().entrySet()) {
            String cmdName = cmdEntry.getKey();
            ICommand cmd = cmdEntry.getValue();
            if (!cmdName.equalsIgnoreCase(cmd.getCommandName())) {
                // skip command alias
                continue;
            }
            String cmdPackageName = cmd.getClass().getPackage().getName();
            boolean foundOwningMod = false;
            for (ModInfo modInfo : modsInfo.values()) {
                if (modInfo.isOwnedPackage(cmdPackageName)) {
                    modInfo.addCommand(cmd, sender);
                    foundOwningMod = true;
                    break;
                }
            }
            if (!foundOwningMod) {
                unknownMod.addCommand(cmd, sender);
            }
        }
        if (unknownMod.getCommandsCount() > 0) {
            modsInfo.put("zzzzzzzz_unknown", unknownMod);
        }
        new TickDelay(() -> Minecraft.getMinecraft().displayGuiScreen(new NumerousCommandsGui(modsInfo.values())), 1);
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
