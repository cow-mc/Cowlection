package eu.olli.cowmoonication.command;

import eu.olli.cowmoonication.Cowmoonication;
import eu.olli.cowmoonication.config.MooConfig;
import eu.olli.cowmoonication.config.MooGuiConfig;
import eu.olli.cowmoonication.util.TickDelay;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;

import java.util.List;
import java.util.Set;

public class MooCommand extends CommandBase {
    private final Cowmoonication main;

    public MooCommand(Cowmoonication main) {
        this.main = main;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            main.getUtils().sendMessage(new ChatComponentTranslation(getCommandUsage(sender)));
            return;
        }
        if (args[0].equalsIgnoreCase("friends") || args[0].equalsIgnoreCase("f")) {
            new TickDelay(() -> Minecraft.getMinecraft().displayGuiScreen(new MooGuiConfig(null)), 1); // delay by 1 tick, because the chat closing would close the new gui instantly as well.
        } else if (args[0].equalsIgnoreCase("list")) {
            handleListBestFriends();
        } else if (args[0].equalsIgnoreCase("toggle")) {
            main.getConfig().toggleNotifications();
            main.getUtils().sendMessage(EnumChatFormatting.GREEN + "Switched all non-best friend login/logout notifications " + (MooConfig.filterFriendNotifications ? EnumChatFormatting.DARK_GREEN + "off" : EnumChatFormatting.DARK_RED + "on"));
        } else if (args[0].equalsIgnoreCase("guiscale")) {
            int currentGuiScale = (Minecraft.getMinecraft()).gameSettings.guiScale;
            if (args.length == 1) {
                main.getUtils().sendMessage(EnumChatFormatting.GREEN + "Current GUI scale: " + EnumChatFormatting.DARK_GREEN + currentGuiScale);
            } else {
                int scale = Math.min(10, MathHelper.parseIntWithDefault(args[1], 6));
                Minecraft.getMinecraft().gameSettings.guiScale = scale;
                main.getUtils().sendMessage(EnumChatFormatting.GREEN + "New GUI scale: " + EnumChatFormatting.DARK_GREEN + scale + EnumChatFormatting.GREEN + " (previous: " + EnumChatFormatting.DARK_GREEN + currentGuiScale + EnumChatFormatting.GREEN + ")");
            }
        } else {
            main.getUtils().sendMessage(new ChatComponentTranslation(getCommandUsage(sender)));
        }
    }

    private void handleListBestFriends() {
        Set<String> bestFriends = main.getFriends().getBestFriends();

        // TODO show fancy gui with list of best friends (maybe just the mod's settings?)
        main.getUtils().sendMessage(EnumChatFormatting.GREEN + "Best friends: " + String.join(", ", bestFriends));
    }

    @Override
    public String getCommandName() {
        return "moo";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return Cowmoonication.MODID + ":command.moo.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "friends", "list", "toggle", "guiscale");
        }
        return null;
    }
}
