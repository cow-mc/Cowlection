package de.cowtipper.cowlection.config.gui;

import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.util.MooChatComponent;
import de.cowtipper.cowlection.util.TickDelay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.GuiScrollingList;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.GuiEditArrayEntries;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.net.URI;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Based on GuiModList
 */
public class MooConfigBestFriendsMigration extends GuiScreen {
    /**
     * <pre>
     * §9§m-----------------------------------------------------
     * §r§a[VIP] PLAYER§r§a is now a best friend!§r§9§m
     * -----------------------------------------------------§r
     *
     * §9§m-----------------------------------------------------
     * §r§a[VIP] PLAYER§r§e is no longer a best friend!§r§9§m
     * -----------------------------------------------------§r
     *
     * §9§m-----------------------------------------------------
     * §r§6[MVP§r§d++§r§6] PLAYER§r§c isn't on your friends list!§r§9§m
     * -----------------------------------------------------§r
     * </pre>
     */
    private static final Pattern BEST_FRIEND_CHAT_PATTERN = Pattern.compile("-{20,}\\s*(?:\\[[^]]+] )?(\\w+)([^\n]+)\\s*-{20,}");

    private BestFriendsListGui bestFriendsGui;
    private GuiButton btnRemoveAll;
    private GuiButton btnHelp;
    private GuiButton btnClose;
    private boolean isMigrating = false;

    @Override
    public void onGuiClosed() {
        MinecraftForge.EVENT_BUS.unregister(this);
        if (Cowlection.getInstance().getFriendsHandler().getBestFriendsListSize() == 0) {
            new TickDelay(() -> Cowlection.getInstance().getChatHelper().sendMessage(new MooChatComponent("[§2Cowlection§a] All Cowlection best friends have been migrated or removed.").green()
                    .appendFreshSibling(new MooChatComponent("You can now use Hypixel's commands instead: §b[open patch notes]").green().setUrl("https://hypixel.net/threads/social-update-online-status-best-friends-more.4638020/")
                            .appendFreshSibling(new MooChatComponent(" §6➊ §aView best friends list: §2/friend list best §aor §2/fl best").green().setSuggestCommand("/fl best"))
                            .appendFreshSibling(new MooChatComponent(" §6➋ §aAdd or remove best friend: §2/friend best <player name>").green().setSuggestCommand("/f best ")))),
                    isMigrating ? 20 : 0);
        }
    }

    // priority = highest to ignore other mods modifying the chat output
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onBestFriendsAddResponse(ClientChatReceivedEvent e) {
        if (e.type == 2 || !isMigrating) return;
        String text = EnumChatFormatting.getTextWithoutFormattingCodes(e.message.getUnformattedText());
        if (!text.startsWith("-----------------------------") || !text.endsWith("-----------------------------"))
            return;
        Matcher matcher = BEST_FRIEND_CHAT_PATTERN.matcher(text);
        if (!matcher.matches()) return;
        String username = matcher.group(1);
        String rawFriendStatus = matcher.group(2);

        int friendStatus;
        if (rawFriendStatus.contains("no longer a best friend")) {
            friendStatus = 0xFFaa0000;
        } else if (rawFriendStatus.contains("now a best friend")) {
            friendStatus = 0xFF00aa00;
        } else if (rawFriendStatus.contains("isn't on your friends list")) {
            friendStatus = 0xFFff5555;
        } else {
            return;
        }

        this.bestFriendsGui.setBestFriendStatus(username, friendStatus);
    }

    @Override
    public void initGui() {
        if (Cowlection.getInstance().getFriendsHandler().getBestFriendsListSize() == 0) {
            mc.displayGuiScreen(null);
            return;
        }
        MinecraftForge.EVENT_BUS.register(this);

        this.buttonList.clear();

        // remove all button
        this.buttonList.add(this.btnRemoveAll = new GuiButtonExt(3, this.width - 69, 4, 20, 20, EnumChatFormatting.DARK_RED + "♺"));

        // help button
        this.buttonList.add(this.btnHelp = new GuiButtonExt(1, this.width - 47, 4, 20, 20, "?"));
        // close button
        this.buttonList.add(this.btnClose = new GuiButtonExt(2, this.width - 25, 4, 20, 20, EnumChatFormatting.RED + "X"));

        // scrollable gui
        this.bestFriendsGui = new BestFriendsListGui(this.width / 2, this.height, this.isMigrating);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawGradientRect(this.width / 2, 0, this.width, this.height, -1072689136, -804253680); // from #drawDefaultBackground
        super.drawScreen(mouseX, mouseY, partialTicks);
        GlStateManager.pushMatrix();
        double scaleFactor = 1.3;
        GlStateManager.scale(scaleFactor, scaleFactor, 0);
        this.drawString(this.fontRendererObj, "Cowlection: Best friends migration", (int) ((this.width / 2 + 8) / scaleFactor), 6, 0xFFCC00);
        GlStateManager.popMatrix();
        this.bestFriendsGui.drawScreen(mouseX, mouseY, partialTicks);
        if (btnRemoveAll.isMouseOver()) {
            List<String> removeAllTooltip = new ArrayList<>();
            removeAllTooltip.add(EnumChatFormatting.RED + "Discard/Remove " + EnumChatFormatting.DARK_RED + "all " + EnumChatFormatting.RED + "Cowlection best friends");
            drawHoveringText(removeAllTooltip, mouseX, mouseY);
        } else if (btnHelp.isMouseOver()) {
            List<String> helpTooltip = new ArrayList<>();
            helpTooltip.add("" + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + "Cowlection: Best friends migration");
            helpTooltip.add(EnumChatFormatting.GREEN + "If you have any questions or need help with the best friends list migration:");
            helpTooltip.add(EnumChatFormatting.GREEN + "Join the Cowshed discord and open a ticket!");
            drawHoveringText(helpTooltip, mouseX, mouseY);
        } else if (btnClose.isMouseOver()) {
            drawHoveringText(Arrays.asList(EnumChatFormatting.RED + "Save & Close", "" + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + "Hint:" + EnumChatFormatting.RESET + " alternatively press ESC"), mouseX, mouseY);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == btnRemoveAll) {
            // show best friends deletion confirmation screen
            GuiYesNo guiDeleteRule = new GuiYesNo(MooConfigBestFriendsMigration.this,
                    EnumChatFormatting.RED + "Discard/remove " + EnumChatFormatting.DARK_RED + Cowlection.getInstance().getFriendsHandler().getBestFriendsListSize() + " (= all)" + EnumChatFormatting.RED + " from Cowlection best friends list?",
                    EnumChatFormatting.RED + "This action cannot be reverted!",
                    1337);
            mc.displayGuiScreen(guiDeleteRule);
        } else if (button == btnHelp) {
            GuiConfirmOpenLink guiHelp = new GuiConfirmOpenLink(this, Cowlection.INVITE_URL, 9001, true);
            guiHelp.disableSecurityWarning();
            mc.displayGuiScreen(guiHelp);
        } else if (button == btnClose) {
            mc.displayGuiScreen(null);
        }
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        super.confirmClicked(result, id);
        if (result) {
            if (id == 1337) {
                Cowlection.getInstance().getFriendsHandler().removeAllBestFriends();
            } else if (id == 9001) {
                try {
                    Desktop.getDesktop().browse(new URI(Cowlection.INVITE_URL));
                } catch (Throwable throwable) {
                    Cowlection.getInstance().getLogger().error("Couldn't open link " + Cowlection.INVITE_URL, throwable);
                }
            } else if (id < 500) {
                // user confirmed rule deletion
                BestFriendsListGui.BestFriendEntry removedBestFriendEntry = this.bestFriendsGui.bestFriendEntries.get(id);
                if (removedBestFriendEntry != null) {
                    removedBestFriendEntry.removeBestFriend();
                }
            }
        }
        mc.displayGuiScreen(this);
    }

    /**
     * Based on GuiModList.Info
     */
    private class BestFriendsListGui extends GuiScrollingList {
        private final MooChatComponent BEST_FRIENDS_MIGRATIONS_GUIDE = new MooChatComponent("The 'best friends list' feature has been removed from this mod, as Hypixel added a very similar feature ").red()
                .appendSibling(new MooChatComponent("[click to open patch notes]")
                        .darkAqua()
                        .setUrl("https://hypixel.net/threads/social-update-online-status-best-friends-more.4638020/"))
                .appendFreshSibling(new MooChatComponent("You can either…\n §e➊ §ftry to add someone from your Cowlection best friends list to your Hypixel best friends list (§2+§f button), or\n §e➋ §fdiscard/remove them from your Cowlection best friends list without trying to add them on Hypixel as a best friend (§c♺§f button)."
                        + "\n§4§l❢ §eOnly players who are already on your §6normal §eHypixel friends list can be added as a best friends."))
                .appendFreshSibling(new MooChatComponent("➜ Start migrating my clicking the button at the top.").gray());

        private final List<IChatComponent> introduction;
        private final List<BestFriendEntry> bestFriendEntries;
        private final boolean isMigrating;
        private final GuiButton startMigratingButton;
        private BestFriendEntry hoveredEntry;

        public BestFriendsListGui(int width, int height, boolean isMigrating) {
            super(MooConfigBestFriendsMigration.this.mc,
                    width - 6, height,
                    27, height - 3, width + 3, 18,
                    MooConfigBestFriendsMigration.this.width, MooConfigBestFriendsMigration.this.height);
            setHeaderInfo(true, isMigrating ? 20 : 30);

            this.bestFriendEntries = isMigrating ? convertToEntries() : Collections.emptyList();
            this.introduction = isMigrating ? Collections.emptyList() : this.resizeContent();
            this.startMigratingButton = isMigrating ? null : new GuiButtonExt(21000, this.left + 25, 0, 170, 20, EnumChatFormatting.DARK_GREEN + "➜ " + EnumChatFormatting.GREEN + "Start best friends migration");
            this.isMigrating = isMigrating;
        }

        private List<BestFriendEntry> convertToEntries() {
            Set<String> bestFriends = Cowlection.getInstance().getFriendsHandler().getBestFriends();

            return bestFriends.stream().map(BestFriendEntry::new).collect(Collectors.toList());
        }

        public void setBestFriendStatus(String username, int friendStatus) {
            for (BestFriendEntry bestFriendEntry : this.bestFriendEntries) {
                if (bestFriendEntry.bestFriend.equals(username)) {
                    bestFriendEntry.setStatus(friendStatus);
                }
            }
        }

        @Override
        protected void drawHeader(int entryRight, int relativeY, Tessellator tess) {
            if (relativeY < 0) return;
            boolean hasEntriesLeft = this.bestFriendEntries.size() > 0;
            if (hasEntriesLeft) {
                MooConfigBestFriendsMigration.this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.GREEN + "Migrate " + EnumChatFormatting.WHITE + "or " + EnumChatFormatting.RED + "remove " + EnumChatFormatting.WHITE + "best friend", this.left + 6, relativeY + 5, 0xFFCC00);
            } else if (!isMigrating && startMigratingButton != null) {
                this.startMigratingButton.yPosition = relativeY + 3;
                this.startMigratingButton.drawButton(mc, mouseX, mouseY);
            }
        }

        @Override
        protected void clickHeader(int x, int y) {
            super.clickHeader(x, y);
            if (this.startMigratingButton.isMouseOver()) {
                MooConfigBestFriendsMigration.this.isMigrating = true;
                MooConfigBestFriendsMigration.this.initGui();
            }
        }

        private List<IChatComponent> resizeContent() {
            return new ArrayList<>(GuiUtilRenderComponents.splitText(BEST_FRIENDS_MIGRATIONS_GUIDE,
                    this.listWidth - 8, MooConfigBestFriendsMigration.this.fontRendererObj, false, true));
        }


        @Override
        protected int getSize() {
            return isMigrating ? this.bestFriendEntries.size() : this.introduction.size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick) {
            if (isMigrating) {
                this.bestFriendEntries.get(index).mousePressed(mouseX, mouseY, index);
            } else {
                IChatComponent line = introduction.get(index);
                if (line != null) {
                    int xOffset = this.left;
                    for (IChatComponent part : line) {
                        if (!(part instanceof ChatComponentText)) {
                            continue;
                        }
                        xOffset += MooConfigBestFriendsMigration.this.fontRendererObj.getStringWidth(((ChatComponentText) part).getChatComponentText_TextValue());
                        if (xOffset >= this.mouseX) {
                            MooConfigBestFriendsMigration.this.handleComponentClick(part);
                            break;
                        }
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
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            hoveredEntry = null;
            super.drawScreen(mouseX, mouseY, partialTicks);
            if (hoveredEntry != null) {
                hoveredEntry.drawToolTip(mouseX, mouseY);
            }
        }

        @Override
        protected void drawSlot(int slotIdx, int entryRight, int slotTop, int slotBuffer, Tessellator tess) {
            if (this.isMigrating) {
                BestFriendEntry bestFriendEntry = bestFriendEntries.get(slotIdx);
                if (bestFriendEntry != null) {
                    bestFriendEntry.drawEntry(this.left + 4, slotTop, this.right - 4);
                    if (mouseY >= slotTop && mouseY < slotTop + this.slotHeight) {
                        // mouse is over this slot
                        hoveredEntry = bestFriendEntry;
                    }
                }
            } else {
                IChatComponent line = introduction.get(slotIdx);
                if (line != null) {
                    MooConfigBestFriendsMigration.this.fontRendererObj.drawStringWithShadow(line.getFormattedText(), this.left + 4, slotTop, 0xFFffffff);
                }
            }
        }

        /**
         * Based on:
         *
         * @see GuiConfigEntries.StringEntry
         * @see GuiEditArrayEntries.BaseEntry
         */
        private class BestFriendEntry {
            private final String bestFriend;
            private final GuiButton btnAdd;
            private final List<String> btnAddTooltip;
            private final GuiButton btnRemove;
            private final List<String> btnRemoveTooltip;
            private int friendStatus = 0xFFffffff;

            public BestFriendEntry(String bestFriend) {
                this.bestFriend = bestFriend;

                this.btnAdd = new GuiButtonExt(50, 0, 0, 18, 16, "+");
                this.btnAdd.packedFGColour = GuiUtils.getColorCode('2', true);
                this.btnAddTooltip = new ArrayList<>();
                this.btnAddTooltip.add("Try to §aadd §e" + bestFriend + " §fto Hypixel best friends list");
                this.btnAddTooltip.add("§7Runs the command §8/friend best " + bestFriend);

                this.btnRemove = new GuiButtonExt(50, 0, 0, 18, 16, "♺");
                this.btnRemove.packedFGColour = GuiUtils.getColorCode('c', true);
                this.btnRemoveTooltip = new ArrayList<>();
                this.btnRemoveTooltip.add("§cRemove §e" + bestFriend + " §ffrom your Cowlection best friends list");
                this.btnRemoveTooltip.add("§7Runs the command §8/moo remove " + bestFriend);
            }

            public void setStatus(int friendStatus) {
                this.friendStatus = friendStatus;
            }

            public void drawEntry(int x, int y, int right) {
                int currentX = x;

                btnAdd.xPosition = currentX;
                btnAdd.yPosition = y - 1;
                btnAdd.drawButton(mc, mouseX, mouseY);
                currentX += btnAdd.width + 3;

                btnRemove.xPosition = currentX;
                btnRemove.yPosition = y - 1;
                btnRemove.drawButton(mc, mouseX, mouseY);
                currentX += btnRemove.width + 3;

                MooConfigBestFriendsMigration.this.fontRendererObj.drawStringWithShadow(this.bestFriend, (currentX + 1), (y + 4), friendStatus);

                if (this.friendStatus < 0xFFffffff) {
                    drawRect(x, y - 1, right, y + 15, 0x99666666);
                }
            }

            public void drawToolTip(int mouseX, int mouseY) {
                if (btnAdd.isMouseOver()) {
                    drawHoveringText(this.btnAddTooltip, mouseX, mouseY);
                } else if (btnRemove.isMouseOver()) {
                    drawHoveringText(btnRemoveTooltip, mouseX, mouseY);
                }
            }

            public void mousePressed(int mouseX, int mouseY, int slotIndex) {
                if (btnAdd.mousePressed(mc, mouseX, mouseY)) {
                    Minecraft.getMinecraft().thePlayer.sendChatMessage("/friend best " + this.bestFriend);
                    this.removeBestFriend();
                } else if (btnRemove.mousePressed(mc, mouseX, mouseY)) {
                    if (isShiftKeyDown()) {
                        this.removeBestFriend();
                    } else {
                        // show best friend deletion confirmation screen
                        GuiYesNo guiDeleteRule = new GuiYesNo(MooConfigBestFriendsMigration.this,
                                EnumChatFormatting.RED + "Discard/remove " + EnumChatFormatting.YELLOW + bestFriend + EnumChatFormatting.RESET + " from Cowlection best friends list?",
                                EnumChatFormatting.GRAY + "Hint: You can hold " + EnumChatFormatting.WHITE + "SHIFT " + EnumChatFormatting.GRAY + "while clicking the " + EnumChatFormatting.RED + "♺ " + EnumChatFormatting.GRAY + "button to skip this confirmation screen.",
                                slotIndex);
                        mc.displayGuiScreen(guiDeleteRule);
                    }
                }
            }

            public void removeBestFriend() {
                Cowlection.getInstance().getFriendsHandler().removeBestFriend(this.bestFriend);
                this.friendStatus = 0xFFbbbbbb;
                if (Cowlection.getInstance().getFriendsHandler().getBestFriendsListSize() == 0) {
                    mc.displayGuiScreen(null);
                }
            }
        }
    }
}
