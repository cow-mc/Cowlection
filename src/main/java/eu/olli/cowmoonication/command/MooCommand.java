package eu.olli.cowmoonication.command;

import eu.olli.cowmoonication.Cowmoonication;
import eu.olli.cowmoonication.config.MooConfig;
import eu.olli.cowmoonication.config.MooGuiConfig;
import eu.olli.cowmoonication.util.TickDelay;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.*;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
        if (args[0].equalsIgnoreCase("friends") || args[0].equalsIgnoreCase("config")) {
            new TickDelay(() -> Minecraft.getMinecraft().displayGuiScreen(new MooGuiConfig(null)), 1); // delay by 1 tick, because the chat closing would close the new gui instantly as well.
        } else if (args[0].equalsIgnoreCase("list")) {
            handleListBestFriends();
        } else if (args[0].equalsIgnoreCase("toggle")) {
            main.getConfig().toggleNotifications();
            main.getUtils().sendMessage(EnumChatFormatting.GREEN + "\u2714 Switched all non-best friend login/logout notifications " + (MooConfig.filterFriendNotifications ? EnumChatFormatting.DARK_GREEN + "off" : EnumChatFormatting.DARK_RED + "on"));
        } else if (args[0].equalsIgnoreCase("guiscale")) {
            int currentGuiScale = (Minecraft.getMinecraft()).gameSettings.guiScale;
            if (args.length == 1) {
                main.getUtils().sendMessage(EnumChatFormatting.GREEN + "\u279C Current GUI scale: " + EnumChatFormatting.DARK_GREEN + currentGuiScale);
            } else {
                int scale = Math.min(10, MathHelper.parseIntWithDefault(args[1], 6));
                Minecraft.getMinecraft().gameSettings.guiScale = scale;
                main.getUtils().sendMessage(EnumChatFormatting.GREEN + "\u2714 New GUI scale: " + EnumChatFormatting.DARK_GREEN + scale + EnumChatFormatting.GREEN + " (previous: " + EnumChatFormatting.DARK_GREEN + currentGuiScale + EnumChatFormatting.GREEN + ")");
            }
        } else if (args[0].equalsIgnoreCase("update")) {
            boolean updateCheckStarted = main.getVersionChecker().runUpdateCheck(true);

            if (updateCheckStarted) {
                main.getUtils().sendMessage(EnumChatFormatting.GREEN + "\u279C Checking for a newer mod version...");
                // VersionChecker#handleVersionStatus will run with a 5 seconds delay
            } else {
                long nextUpdate = main.getVersionChecker().getNextCheck();
                String waitingTime = String.format("%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(nextUpdate),
                        TimeUnit.MILLISECONDS.toSeconds(nextUpdate) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(nextUpdate)));
                main.getUtils().sendMessage(new ChatComponentText("\u26A0 Update checker is on cooldown. Please wait " + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + waitingTime + EnumChatFormatting.RESET + EnumChatFormatting.RED + " more minutes before checking again.").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            }
        } else if (args[0].equalsIgnoreCase("updateHelp")) {
            main.getUtils().sendMessage(new ChatComponentText("\u279C Update instructions:").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(true))
                    .appendSibling(new ChatComponentText("\n\u278A" + EnumChatFormatting.YELLOW + " download latest mod version").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false)
                            .setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, main.getVersionChecker().getDownloadUrl()))
                            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Download the latest version of Cowmoonication\n\u279C Click to download latest mod file")))))
                    .appendSibling(new ChatComponentText("\n\u278B" + EnumChatFormatting.YELLOW + " exit Minecraft").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false)
                            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.GOLD + "\u278B" + EnumChatFormatting.YELLOW + " Without closing Minecraft first,\n" + EnumChatFormatting.YELLOW + "you can't delete the old .jar file!")))))
                    .appendSibling(new ChatComponentText("\n\u278C" + EnumChatFormatting.YELLOW + " copy " + EnumChatFormatting.GOLD + "Cowmoonication-" + main.getVersionChecker().getNewVersion() + ".jar" + EnumChatFormatting.YELLOW + " into mods folder").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false)
                            .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/moo folder"))
                            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Open mods folder with command " + EnumChatFormatting.GOLD + "/moo folder\n\u279C Click to open mods folder")))))
                    .appendSibling(new ChatComponentText("\n\u278D" + EnumChatFormatting.YELLOW + " delete old mod file " + EnumChatFormatting.GOLD + "Cowmoonication-" + Cowmoonication.VERSION + ".jar ").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false)))
                    .appendSibling(new ChatComponentText("\n\u278E" + EnumChatFormatting.YELLOW + " start Minecraft again").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(false))));
        } else if (args[0].equalsIgnoreCase("version")) {
            main.getVersionChecker().handleVersionStatus(true);
        } else if (args[0].equalsIgnoreCase("folder")) {
            try {
                Desktop.getDesktop().open(main.getUtils().getModsFolder());
            } catch (IOException e) {
                main.getUtils().sendMessage(new ChatComponentText("\u2716 An error occurred trying to open the mod's folder. I guess you have to open it manually \u00af\\_(\u30c4)_/\u00af").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
                e.printStackTrace();
            }
        } else {
            main.getUtils().sendMessage(new ChatComponentTranslation(getCommandUsage(sender)));
        }
    }

    private void handleListBestFriends() {
        Set<String> bestFriends = main.getFriends().getBestFriends();

        // TODO show fancy gui with list of best friends (maybe just the mod's settings?)
        main.getUtils().sendMessage(EnumChatFormatting.GREEN + "\u279C Best friends: " + EnumChatFormatting.DARK_GREEN + String.join(EnumChatFormatting.GREEN + ", " + EnumChatFormatting.DARK_GREEN, bestFriends));
    }

    @Override
    public String getCommandName() {
        return "moo";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        IChatComponent usage = new ChatComponentText("\u279C Cowmoonication commands:").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(true))
                .appendSibling(createCmdHelpSection(1, "Login/Logout Notifications"))
                .appendSibling(createCmdHelpEntry("friends", "Add/remove best friends"))
                .appendSibling(createCmdHelpEntry("list", "View list of best friends"))
                .appendSibling(createCmdHelpEntry("toggle", "Toggle show/hide all join/leave notifications"))
                .appendSibling(createCmdHelpSection(2, "Miscellaneous"))
                .appendSibling(createCmdHelpEntry("config", "Open configuration GUI"))
                .appendSibling(createCmdHelpEntry("guiScale", "Change GUI scale"))
                .appendSibling(createCmdHelpSection(3, "Update mod"))
                .appendSibling(createCmdHelpEntry("update", "Check for new mod updates"))
                .appendSibling(createCmdHelpEntry("updateHelp", "Show mod update instructions"))
                .appendSibling(createCmdHelpEntry("version", "View results of last mod update check"))
                .appendSibling(createCmdHelpEntry("folder", "Open Minecraft's mods folder"));
        sender.addChatMessage(usage);
        return "";
    }

    private IChatComponent createCmdHelpSection(int nr, String title) {
        String prefix = Character.toString((char) (0x2789 + nr));
        return new ChatComponentText("\n").appendSibling(new ChatComponentText(prefix + " " + title).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD).setBold(true)));
    }

    private IChatComponent createCmdHelpEntry(String cmd, String usage) {
        String command = "/" + this.getCommandName() + " " + cmd;
        ChatStyle clickableMsg = new ChatStyle()
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Run " + EnumChatFormatting.GOLD + command)))
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
        return new ChatComponentText("\n").appendSibling(new ChatComponentText(command).setChatStyle(clickableMsg.createDeepCopy().setColor(EnumChatFormatting.GOLD)))
                .appendSibling(new ChatComponentText(" \u27A1 " + usage).setChatStyle(clickableMsg.createDeepCopy().setColor(EnumChatFormatting.YELLOW)));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "config", "friends", "list", "toggle", "guiscale", "update", "updateHelp", "version", "folder", "help");
        }
        return null;
    }
}
