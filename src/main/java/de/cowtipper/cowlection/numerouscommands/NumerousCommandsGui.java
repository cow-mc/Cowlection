package de.cowtipper.cowlection.numerouscommands;


import com.google.common.base.Joiner;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.command.CommandBase;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.client.GuiScrollingList;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Based on GuiModList
 */
public class NumerousCommandsGui extends GuiScreen {
    private final List<String> lines;
    private CommandsListGui commandsList;
    private GuiButtonExt btnClose;
    private static float lastScrollDistance;

    public NumerousCommandsGui(Collection<ModInfo> modsInfo) {
        this.lines = new ArrayList<>();
        String unknown = EnumChatFormatting.ITALIC + "unknown";
        for (ModInfo modInfo : modsInfo) {
            if (!lines.isEmpty()) {
                lines.add(null);
                lines.add("" + EnumChatFormatting.GRAY + EnumChatFormatting.BOLD + EnumChatFormatting.STRIKETHROUGH + "--------------------------------------------------");
                lines.add(null);
            }
            lines.add("" + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + modInfo.getName());
            ModMetadata modMetadata = modInfo.getModMetadata();
            if (modMetadata != null) {
                int authorsCount = modMetadata.authorList.size();
                addKeyValue("Author" + (authorsCount > 1 ? "s" : ""), (authorsCount == 0 ? unknown : CommandBase.joinNiceStringFromCollection(modMetadata.authorList)), true);
                addKeyValue("Version", (modMetadata.version.isEmpty() ? unknown : modMetadata.version) + (modInfo.hasUpdate() ? EnumChatFormatting.GREEN + " (update available!)" : ""), true);
                if (!modMetadata.url.isEmpty()) {
                    addKeyValue("URL", modMetadata.url, true);
                }
            }
            lines.add("" + EnumChatFormatting.UNDERLINE + EnumChatFormatting.ITALIC + "Commands" + EnumChatFormatting.RESET + EnumChatFormatting.GRAY + " (" + modInfo.getCommandsCount() + ")");
            Collection<CommandInfo> commands = modInfo.getCommands();
            if (commands.isEmpty()) {
                lines.add(EnumChatFormatting.YELLOW + " ‣ This mod either has no client-side commands, or doesn't use Forge's command registry");
            } else {
                for (CommandInfo cmd : commands) {
                    String cmdNameAndAliases = EnumChatFormatting.YELLOW + " ‣ /" + cmd.getName();
                    if (cmd.getAliases().size() > 0) {
                        cmdNameAndAliases += EnumChatFormatting.DARK_GRAY + " (" + (cmd.getAliases().size() == 1 ? "alias" : "aliases") + ": " + EnumChatFormatting.GRAY + Joiner.on(", ").join(cmd.getAliases()) + EnumChatFormatting.DARK_GRAY + ")";
                    }
                    lines.add(cmdNameAndAliases);
                    String cmdUsage = cmd.getUsage();
                    if (cmdUsage != null) {
                        if (cmdUsage.contains("\n")) {
                            addKeyValue("Usage", "", false);
                            for (String usageLine : cmdUsage.split("\n")) {
                                lines.add("       " + usageLine);
                            }
                        } else {
                            addKeyValue("Usage", cmdUsage, false);
                        }
                    } else if (cmd.isListCommandsCommand()) {
                        addKeyValue("Usage", EnumChatFormatting.GREEN + "You have just used this command to open this GUI", false);
                    }
                }
            }
        }
    }

    private void addKeyValue(String key, String value, boolean light) {
        EnumChatFormatting colorCodeKey = light ? EnumChatFormatting.GRAY : EnumChatFormatting.DARK_GRAY;
        EnumChatFormatting colorCodeValue = light ? EnumChatFormatting.RESET : EnumChatFormatting.GRAY;
        lines.add("    " + colorCodeKey + key + ":" + colorCodeValue + " " + value);
    }

    @Override
    public void initGui() {
        // close button
        this.buttonList.add(this.btnClose = new GuiButtonExt(1, this.width - 25, 4, 22, 20, EnumChatFormatting.RED + "X"));
        updateLastScrollDistance();
        // scrollable commands list
        commandsList = new CommandsListGui(this.width - 30, this.lines, lastScrollDistance);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        GlStateManager.pushMatrix();
        double scaleFactor = 1.5;
        GlStateManager.scale(scaleFactor, scaleFactor, 0);
        this.drawString(this.fontRendererObj, "All client-side commands", 30, 6, 0xFFCC00);
        GlStateManager.popMatrix();
        this.commandsList.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == btnClose) {
            this.mc.displayGuiScreen(null);
        }
    }

    @Override
    public void onGuiClosed() {
        updateLastScrollDistance();
    }

    private void updateLastScrollDistance() {
        if (this.commandsList != null) {
            try {
                lastScrollDistance = ReflectionHelper.getPrivateValue(GuiScrollingList.class, this.commandsList, "scrollDistance");
            } catch (ReflectionHelper.UnableToAccessFieldException ignored) {
                lastScrollDistance = 0;
            }
        }
    }

    /**
     * Based on GuiModList.Info
     */
    private class CommandsListGui extends GuiScrollingList {
        private final List<IChatComponent> lines;

        public CommandsListGui(int width, List<String> lines, float lastScrollDistance) {
            super(NumerousCommandsGui.this.mc,
                    width,
                    NumerousCommandsGui.this.height,
                    30, NumerousCommandsGui.this.height - 5, 12, 11,
                    NumerousCommandsGui.this.width,
                    NumerousCommandsGui.this.height);
            this.lines = resizeContent(lines);
            try {
                // scroll to previous location
                ReflectionHelper.setPrivateValue(GuiScrollingList.class, this, lastScrollDistance, "scrollDistance");
            } catch (ReflectionHelper.UnableToAccessFieldException ignored) {
            }
        }

        private List<IChatComponent> resizeContent(List<String> lines) {
            List<IChatComponent> ret = new ArrayList<>();
            for (String line : lines) {
                if (line == null) {
                    ret.add(null);
                    continue;
                }
                IChatComponent chat = ForgeHooks.newChatWithLinks(line, false);
                ret.addAll(GuiUtilRenderComponents.splitText(chat, this.listWidth - 8, NumerousCommandsGui.this.fontRendererObj, false, true));
            }
            return ret;
        }

        @Override
        protected int getSize() {
            return lines != null ? lines.size() : 0;
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick) {
            IChatComponent line = lines.get(index);
            if (line != null) {
                int xOffset = this.left;
                for (IChatComponent part : line) {
                    if (!(part instanceof ChatComponentText)) {
                        continue;
                    }
                    xOffset += NumerousCommandsGui.this.fontRendererObj.getStringWidth(((ChatComponentText) part).getChatComponentText_TextValue());
                    if (xOffset >= this.mouseX) {
                        NumerousCommandsGui.this.handleComponentClick(part);
                        break;
                    }
                }
            }
        }

        @Override
        protected boolean isSelected(int index) {
            return false;
        }

        @Override
        protected void drawBackground() {
        }

        @Override
        protected void drawSlot(int slotIdx, int entryRight, int slotTop, int slotBuffer, Tessellator tess) {
            IChatComponent line = lines.get(slotIdx);
            if (line != null) {
                GlStateManager.enableBlend();
                NumerousCommandsGui.this.fontRendererObj.drawStringWithShadow(line.getFormattedText(), this.left + 4, slotTop, 0xFFFFFF);
                GlStateManager.disableAlpha();
                GlStateManager.disableBlend();
            }
        }
    }
}
