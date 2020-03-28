package eu.olli.cowmoonication.util;

import eu.olli.cowmoonication.Cowmoonication;
import eu.olli.cowmoonication.config.MooConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.Loader;

import java.util.concurrent.TimeUnit;

/**
 * @see ForgeVersion
 */
public class VersionChecker {
    /**
     * Cooldown between to update checks in minutes
     */
    private static final int CHECK_COOLDOWN = 15;
    private static final String CHANGELOG_URL = "https://github.com/cow-mc/Cowmoonication/blob/master/CHANGELOG.md";
    private final Cowmoonication main;
    private long lastCheck;
    private String newVersion;
    private String downloadUrl;

    public VersionChecker(Cowmoonication main) {
        this.main = main;
        this.lastCheck = Minecraft.getSystemTime();
        newVersion = "[newVersion]";
        downloadUrl = "https://github.com/cow-mc/Cowmoonication/releases";
    }

    public boolean runUpdateCheck(boolean isCommandTriggered) {
        if (isCommandTriggered || (!ForgeModContainer.disableVersionCheck && MooConfig.doUpdateCheck)) {
            Runnable handleResults = () -> main.getVersionChecker().handleVersionStatus(isCommandTriggered);

            long now = Minecraft.getSystemTime();

            // only re-run if last check was >CHECK_COOLDOWN minutes ago#
            if (getNextCheck() < 0) { // next allowed check is "in the past", so we're good to go
                lastCheck = now;
                ForgeVersion.startVersionCheck();

                // check status after 5 seconds - hopefully that's enough to check
                new TickDelay(handleResults, 5 * 20);
                return true;
            } else {
                new TickDelay(handleResults, 1);
            }
        }
        return false;
    }

    public void handleVersionStatus(boolean isCommandTriggered) {
        ForgeVersion.CheckResult versionResult = ForgeVersion.getResult(Loader.instance().activeModContainer());
        if (versionResult.target != null) {
            newVersion = versionResult.target.toString();
            downloadUrl = "https://github.com/cow-mc/Cowmoonication/releases/download/v" + newVersion + "/" + Cowmoonication.MODNAME + "-" + newVersion + ".jar";
        }

        IChatComponent statusMsg = null;

        if (isCommandTriggered) {
            if (versionResult.status == ForgeVersion.Status.UP_TO_DATE) {
                // up to date
                statusMsg = new ChatComponentText("\u2714 You're running the latest version (" + Cowmoonication.VERSION + ").").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN));
            } else if (versionResult.status == ForgeVersion.Status.PENDING) {
                // pending
                statusMsg = new ChatComponentText("\u279C " + "Version check either failed or is still running.").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW))
                        .appendSibling(new ChatComponentText("\n \u278A Check for results again in a few seconds with " + EnumChatFormatting.GOLD + "/moo version").setChatStyle(new ChatStyle()
                                .setColor(EnumChatFormatting.YELLOW)
                                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/moo version"))
                                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Run " + EnumChatFormatting.GOLD + "/moo version")))))
                        .appendSibling(new ChatComponentText("\n \u278B Re-run update check with " + EnumChatFormatting.GOLD + "/moo update").setChatStyle(new ChatStyle()
                                .setColor(EnumChatFormatting.YELLOW)
                                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/moo update"))
                                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Run " + EnumChatFormatting.GOLD + "/moo update")))));
            } else if (versionResult.status == ForgeVersion.Status.FAILED) {
                // check failed
                statusMsg = new ChatComponentText("\u2716 Version check failed for an unknown reason. Check again in a few seconds with ").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED))
                        .appendSibling(new ChatComponentText("/moo update").setChatStyle(new ChatStyle()
                                .setColor(EnumChatFormatting.GOLD)
                                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/moo update"))
                                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Run " + EnumChatFormatting.GOLD + "/moo update")))));
            }
        }
        if (versionResult.status == ForgeVersion.Status.OUTDATED || versionResult.status == ForgeVersion.Status.BETA_OUTDATED) {
            // outdated
            IChatComponent spacer = new ChatComponentText(" ").setChatStyle(new ChatStyle().setParentStyle(null));

            IChatComponent text = new ChatComponentText("\u279C New version of " + EnumChatFormatting.DARK_GREEN + "Cowmoonication " + EnumChatFormatting.GREEN + "available (" + Cowmoonication.VERSION + " \u27A1 " + newVersion + ")\n").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN));

            IChatComponent download = new ChatComponentText("[Download]").setChatStyle(new ChatStyle()
                    .setColor(EnumChatFormatting.DARK_GREEN).setBold(true)
                    .setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, downloadUrl))
                    .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Download the latest version of Cowmoonication"))));

            IChatComponent changelog = new ChatComponentText("[Changelog]").setChatStyle(new ChatStyle()
                    .setColor(EnumChatFormatting.DARK_AQUA).setBold(true)
                    .setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, CHANGELOG_URL))
                    .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "View changelog"))));

            IChatComponent updateInstructions = new ChatComponentText("[Update instructions]").setChatStyle(new ChatStyle()
                    .setColor(EnumChatFormatting.GOLD).setBold(true)
                    .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/moo updateHelp"))
                    .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Run " + EnumChatFormatting.GOLD + "/moo updateHelp"))));

            IChatComponent openModsFolder = new ChatComponentText("\n[Open Mods folder]").setChatStyle(new ChatStyle()
                    .setColor(EnumChatFormatting.GREEN).setBold(true)
                    .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/moo folder"))
                    .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Open mods folder with command " + EnumChatFormatting.GOLD + "/moo folder\n\u279C Click to open mods folder"))));

            statusMsg = text.appendSibling(download).appendSibling(spacer).appendSibling(changelog).appendSibling(spacer).appendSibling(updateInstructions).appendSibling(spacer).appendSibling(openModsFolder);
        }

        if (statusMsg != null) {
            main.getChatHelper().sendMessage(statusMsg);
        }
    }

    public long getNextCheck() {
        long cooldown = TimeUnit.MINUTES.toMillis(CHECK_COOLDOWN);
        long systemTime = Minecraft.getSystemTime();
        return cooldown - (systemTime - lastCheck);
    }

    public String getNewVersion() {
        return newVersion;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}
