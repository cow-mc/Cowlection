package de.cowtipper.cowlection.chesttracker;

import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.search.GuiTooltip;
import de.cowtipper.cowlection.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.IOException;
import java.util.*;

public class ChestOverviewGui extends GuiScreen {
    private ItemOverview itemOverview;
    private List<GuiTooltip> guiTooltips;
    private GuiButton btnClose;
    private GuiButton btnUpdateBazaar;
    private GuiButton btnCopy;
    private GuiCheckBox showNonBazaarItems;
    private GuiButton btnBazaarInstantOrOffer;
    private AbortableRunnable updateBazaar;
    private final String screenTitle;
    private final Cowlection main;

    public ChestOverviewGui(Cowlection main) {
        this.screenTitle = Cowlection.MODNAME + " Chest Analyzer";
        this.main = main;
    }

    @Override
    public void initGui() {
        this.guiTooltips = new ArrayList<>();
        // close
        this.buttonList.add(this.btnClose = new GuiButtonExt(1, this.width - 25, 3, 22, 20, EnumChatFormatting.RED + "X"));
        addTooltip(btnClose, Arrays.asList(EnumChatFormatting.RED + "Close interface", "" + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + "Hint:" + EnumChatFormatting.RESET + " alternatively press ESC"));
        // update bazaar prices
        this.buttonList.add(this.btnUpdateBazaar = new GuiButton(20, this.width - 165, 5, 130, 16, "⟳ Update Bazaar prices"));
        addTooltip(btnUpdateBazaar, Arrays.asList(EnumChatFormatting.YELLOW + "Get latest Bazaar prices from Hypixel API", EnumChatFormatting.WHITE + "(only once per minute)"));
        // copy to clipboard
        this.buttonList.add(this.btnCopy = new GuiButton(21, this.width - 280, 5, 110, 16, "⎘ Copy to clipboard"));
        addTooltip(btnCopy, Collections.singletonList(EnumChatFormatting.YELLOW + "Copied data can be pasted into e.g. Google Spreadsheets"));
        // checkbox: show/hide non-bazaar items
        this.buttonList.add(this.showNonBazaarItems = new GuiCheckBox(10, this.width - 162, this.height - 28, " Show non-Bazaar items", MooConfig.chestAnalyzerShowNonBazaarItems));
        addTooltip(showNonBazaarItems, Collections.singletonList(EnumChatFormatting.YELLOW + "Should items that are " + EnumChatFormatting.GOLD + "not " + EnumChatFormatting.YELLOW + "on the Bazaar be displayed?"));
        // toggle: use insta-sell or sell offer prices
        this.buttonList.add(this.btnBazaarInstantOrOffer = new GuiButton(15, this.width - 165, this.height - 16, 130, 14, MooConfig.useInstantSellBazaarPrices() ? "Use Instant-Sell prices" : "Use Sell Offer prices"));
        addTooltip(btnBazaarInstantOrOffer, Collections.singletonList(EnumChatFormatting.YELLOW + "Use " + EnumChatFormatting.GOLD + "Instant-Sell " + EnumChatFormatting.YELLOW + "or " + EnumChatFormatting.GOLD + "Sell Offer" + EnumChatFormatting.YELLOW + " prices?"));
        // main item gui
        this.itemOverview = new ItemOverview();
    }

    private <T extends Gui> void addTooltip(T field, List<String> tooltip) {
        GuiTooltip guiTooltip = new GuiTooltip(field, tooltip);
        this.guiTooltips.add(guiTooltip);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        btnUpdateBazaar.enabled = updateBazaar == null && main.getChestTracker().allowUpdateBazaar();
        itemOverview.drawScreen(mouseX, mouseY, partialTicks);
        this.drawString(this.fontRendererObj, this.screenTitle, itemOverview.getLeftX(), 10, 0xFFCC00);
        super.drawScreen(mouseX, mouseY, partialTicks);
        itemOverview.drawScreenPost(mouseX, mouseY);
        for (GuiTooltip guiTooltip : guiTooltips) {
            if (guiTooltip.checkHover(mouseX, mouseY)) {
                GuiHelper.drawHoveringText(guiTooltip.getText(), mouseX, mouseY, width, height, 300);
                // only one tooltip can be displayed at a time: break!
                break;
            }
        }
    }

    @Override
    public void drawDefaultBackground() {
    }

    @Override
    public void drawWorldBackground(int tint) {
    }

    @Override
    public void drawBackground(int tint) {
        // = dirt background
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.enabled) {
            if (button == btnClose) {
                this.mc.displayGuiScreen(null);
            } else if (button == btnUpdateBazaar) {
                btnUpdateBazaar.enabled = false;
                this.main.getChestTracker().refreshBazaarCache();
                updateBazaar = new AbortableRunnable() {
                    private int retries = 20 * 20; // retry for up to 20 seconds
                    private final long previousBazaarUpdate = main.getChestTracker().getLastBazaarUpdate();

                    @SubscribeEvent
                    public void onTickCheckBazaarDataUpdated(TickEvent.ClientTickEvent e) {
                        if (!stopped && e.phase == TickEvent.Phase.END) {
                            if (Minecraft.getMinecraft().theWorld == null || retries <= 0) {
                                // already stopped; or world gone, probably disconnected; or no retries left (took too long [20 seconds not enough?] or is not on SkyBlock): stop!
                                stop();
                                return;
                            }
                            retries--;
                            if (previousBazaarUpdate == main.getChestTracker().getLastBazaarUpdate()) {
                                // bazaar data wasn't updated yet, retry next tick
                                return;
                            }
                            // refresh item overview
                            Minecraft.getMinecraft().addScheduledTask(() -> ChestOverviewGui.this.itemOverview.reloadItemData());
                            stop();
                        }
                    }

                    @Override
                    public void stop() {
                        if (!stopped) {
                            stopped = true;
                            retries = -1;
                            MinecraftForge.EVENT_BUS.unregister(this);
                            stopScoreboardChecker();
                        }
                    }

                    @Override
                    public void run() {
                        MinecraftForge.EVENT_BUS.register(this);
                    }
                };
                new TickDelay(updateBazaar, 20); // 2 second delay + retrying for 20 seconds, making sure bazaar data got updated
            } else if (button == showNonBazaarItems) {
                this.itemOverview.reloadItemData();
            } else if (button == btnCopy) {
                StringBuilder allItemData = new StringBuilder("Item\tItem (formatted)\tAmount\tPrice (instant-sell)\tValue (instant-sell)\tPrice (sell offer)\tValue (sell offer)");
                for (ItemData itemData : itemOverview.itemDataHolder) {
                    allItemData.append(itemData.toCopyableFormat());
                }
                allItemData.append("\n\n").append("Bazaar value (instant-sell):\t").append(itemOverview.summedValueInstaSell)
                        .append("\n").append("Bazaar value (sell offer):\t").append(itemOverview.summedValueSellOffer);
                GuiScreen.setClipboardString(allItemData.toString());
            } else if (button == btnBazaarInstantOrOffer) {
                if ("Use Instant-Sell prices".equals(btnBazaarInstantOrOffer.displayString)) {
                    btnBazaarInstantOrOffer.displayString = "Use Sell Offer prices";
                } else {
                    btnBazaarInstantOrOffer.displayString = "Use Instant-Sell prices";
                }
                this.itemOverview.reloadItemData();
            }
        }
    }

    private void stopScoreboardChecker() {
        if (updateBazaar != null) {
            // there is still a bazaar update-checker running, stop it
            updateBazaar.stop();
            updateBazaar = null;
        }
    }

    @Override
    public void onGuiClosed() {
        if (MooConfig.chestAnalyzerShowCommandUsage) {
            main.getChatHelper().sendMessage(new MooChatComponent(EnumChatFormatting.GRAY + "Use " + EnumChatFormatting.WHITE + "/moo analyzeChests stop " + EnumChatFormatting.GRAY + "to stop the Chest Analyzer.").setSuggestCommand("/moo analyzeChests stop")
                    .appendFreshSibling(new MooChatComponent(EnumChatFormatting.GRAY + "Or continue adding more chests and run " + EnumChatFormatting.WHITE + "/moo analyzeChests " + EnumChatFormatting.GRAY + " again to re-run the analysis.").setSuggestCommand("/moo analyzeChests")));
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        if (this.itemOverview != null) {
            this.itemOverview.handleMouseInput();
        }
    }

    /**
     * Inspired by {@link net.minecraft.client.gui.achievement.GuiStats}
     */
    class ItemOverview extends GuiSlot {
        private Column orderBy = Column.PRICE_SUM;
        private boolean orderDesc = true;
        private long lastOrderChange;
        private List<ItemData> itemDataHolder;
        private int summedValueInstaSell;
        private int summedValueSellOffer;

        ItemOverview() {
            super(ChestOverviewGui.this.mc, ChestOverviewGui.this.width, ChestOverviewGui.this.height, 32, ChestOverviewGui.this.height - 32, 16);
            this.setShowSelectionBox(false);
            // space above first entry for control buttons
            int headerPadding = 20;
            this.setHasListHeader(true, headerPadding);

            reloadItemData();
        }

        private void reloadItemData() {
            boolean useInstantSellPrices = "Use Instant-Sell prices".equals(btnBazaarInstantOrOffer.displayString);
            itemDataHolder = main.getChestTracker().getAnalysisResult(orderBy, orderDesc, useInstantSellPrices);
            summedValueInstaSell = 0;
            summedValueSellOffer = 0;
            boolean showNonBazaarItems = ChestOverviewGui.this.showNonBazaarItems.isChecked();

            for (Iterator<ItemData> iterator = itemDataHolder.iterator(); iterator.hasNext(); ) {
                ItemData itemData = iterator.next();
                boolean hasBazaarPrice = false;
                if (itemData.getBazaarInstantSellPrice() > 0) {
                    summedValueInstaSell += itemData.getBazaarInstantSellValue();
                    hasBazaarPrice = true;
                }
                if (itemData.getBazaarSellOfferPrice() > 0) {
                    summedValueSellOffer += itemData.getBazaarSellOfferValue();
                    hasBazaarPrice = true;
                }
                if (!showNonBazaarItems && !hasBazaarPrice) {
                    iterator.remove();
                }
            }
        }

        @Override
        protected void drawListHeader(int x, int y, Tessellator tessellator) {
            if (y < 0) {
                // header not on screen
                return;
            }
            int arrowX = -50;
            // draw column titles
            for (Column column : Column.values()) {
                int columnX = x + column.getXOffset() - (column != Column.ITEM_NAME ? ChestOverviewGui.this.fontRendererObj.getStringWidth(column.getName()) : /* item name is aligned left, rest right */ 0);
                ChestOverviewGui.this.drawString(ChestOverviewGui.this.fontRendererObj, column.getName(), columnX, y + 2, 0xFFFFFF);
                if (column == orderBy) {
                    arrowX = columnX;
                }
            }
            // draw arrow down/up
            GuiHelper.drawSprite(arrowX - 18, y - 3, 18 + (orderDesc ? 0 : 18), 0, 500);
        }

        @Override
        public void actionPerformed(GuiButton button) {
            super.actionPerformed(button);
        }

        @Override
        protected int getSize() {
            return this.itemDataHolder.size();
        }

        /**
         * GuiSlot#clickedHeader
         */
        @Override
        protected void func_148132_a(int x, int y) {
            long now = System.currentTimeMillis();
            if (now - lastOrderChange < 50) {
                // detected two clicks
                return;
            }
            lastOrderChange = now;

            int allowedMargin = 1; // tolerance to the left and right of a word to still be considered a click
            for (Column column : Column.values()) {
                int xOffset = getLeftX() + column.getXOffset();
                int columnTitleWidth = ChestOverviewGui.this.fontRendererObj.getStringWidth(column.getName());
                int columnXMin;
                int columnXMax;
                if (column == Column.ITEM_NAME) {
                    // aligned left
                    columnXMin = xOffset - /* up/down arrow */ 16;
                    columnXMax = xOffset + columnTitleWidth;
                } else {
                    // aligned right
                    columnXMin = xOffset - columnTitleWidth - /* up/down arrow */ 16;
                    columnXMax = xOffset;
                }

                if (mouseX + allowedMargin >= columnXMin && mouseX - allowedMargin <= columnXMax) {
                    // clicked on column title!
                    orderDesc = orderBy != column || !orderDesc;
                    orderBy = column;
                    mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
                    break;
                }
            }
            reloadItemData();
        }

        @Override
        protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
        }

        @Override
        protected boolean isSelected(int slotIndex) {
            return false;
        }

        @Override
        protected void drawBackground() {
        }

        @Override
        public int getListWidth() {
            return this.width - 30;
        }

        @Override
        protected void drawSlot(int entryID, int x, int y, int z, int mouseXIn, int mouseYIn) {
            if (!isMouseYWithinSlotBounds(y + 5)) {
                // slot isn't visible anyways...
                return;
            }
            ItemData itemData = itemDataHolder.get(entryID);

            // render item icon without shadows
            GlStateManager.enableRescaleNormal();
            RenderHelper.enableGUIStandardItemLighting();
            ChestOverviewGui.this.itemRender.renderItemIntoGUI(itemData.getItemStack(), x, y - 3);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();

            FontRenderer fontRenderer = ChestOverviewGui.this.fontRendererObj;
            String itemAmount = Utils.formatNumber(itemData.getAmount());
            int amountXPos = x + Column.ITEM_AMOUNT.getXOffset() - fontRenderer.getStringWidth(itemAmount);
            int itemNameXPos = x + Column.ITEM_NAME.getXOffset();
            String itemName = fontRenderer.trimStringToWidth(itemData.getName(), amountXPos - itemNameXPos - 5);
            if (itemName.length() != itemData.getName().length()) {
                itemName += "…";
            }
            ChestOverviewGui.this.drawString(fontRenderer, itemName, itemNameXPos, y + 1, entryID % 2 == 0 ? 0xFFFFFF : 0x909090);
            ChestOverviewGui.this.drawString(fontRenderer, itemAmount, amountXPos, y + 1, entryID % 2 == 0 ? 0xFFFFFF : 0x909090);

            boolean useInstantSellPrices = "Use Instant-Sell prices".equals(btnBazaarInstantOrOffer.displayString);
            double itemPrice = useInstantSellPrices ? itemData.getBazaarInstantSellPrice() : itemData.getBazaarSellOfferPrice();
            String bazaarPrice = itemPrice > 0 ? Utils.formatDecimal(itemPrice) : EnumChatFormatting.DARK_GRAY + "?";
            ChestOverviewGui.this.drawString(fontRenderer, bazaarPrice, x + Column.PRICE_EACH.getXOffset() - fontRenderer.getStringWidth(bazaarPrice), y + 1, entryID % 2 == 0 ? 0xFFFFFF : 0x909090);

            double itemValue = useInstantSellPrices ? itemData.getBazaarInstantSellValue() : itemData.getBazaarSellOfferValue();
            String bazaarValue = itemPrice > 0 ? Utils.formatNumber(itemValue) : EnumChatFormatting.DARK_GRAY + "?";
            ChestOverviewGui.this.drawString(fontRenderer, bazaarValue, x + Column.PRICE_SUM.getXOffset() - fontRenderer.getStringWidth(bazaarValue), y + 1, entryID % 2 == 0 ? 0xFFFFFF : 0x909090);
        }

        public void drawScreenPost(int mouseX, int mouseY) {
            int xMin = getLeftX();
            String bazaarValueInstantSell = "∑ Bazaar value (instant-sell prices): " + (summedValueInstaSell > 0 ? EnumChatFormatting.WHITE + Utils.formatNumber(summedValueInstaSell) : EnumChatFormatting.DARK_GRAY + "?"); // sum
            ChestOverviewGui.this.drawString(ChestOverviewGui.this.fontRendererObj, bazaarValueInstantSell, xMin + 175 - ChestOverviewGui.this.fontRendererObj.getStringWidth("∑ Bazaar value (instant-sell prices): "), ChestOverviewGui.this.height - 28, 0xdddddd);
            String bazaarValueSellOffer = "∑ Bazaar value (sell offer prices): " + (summedValueSellOffer > 0 ? EnumChatFormatting.WHITE + Utils.formatNumber(summedValueSellOffer) : EnumChatFormatting.DARK_GRAY + "?"); // sum
            ChestOverviewGui.this.drawString(ChestOverviewGui.this.fontRendererObj, bazaarValueSellOffer, xMin + 175 - ChestOverviewGui.this.fontRendererObj.getStringWidth("∑ Bazaar value (sell offer prices): "), ChestOverviewGui.this.height - 17, 0xdddddd);

            if (isMouseYWithinSlotBounds(mouseY)) {
                int slotIndex = this.getSlotIndexFromScreenCoords(mouseX, mouseY);

                if (slotIndex >= 0) {
                    // mouse is over a slot: maybe draw item tooltip
                    int xMax = xMin + 16; // 16 = item icon width
                    if (mouseX < xMin || mouseX > xMax) {
                        // mouseX outside of valid item x values
                        return;
                    }
                    ItemData itemData = itemDataHolder.get(slotIndex);
                    FontRenderer font = itemData.getItemStack().getItem().getFontRenderer(itemData.getItemStack());
                    GlStateManager.pushMatrix();
                    ChestOverviewGui.this.drawHoveringText(itemData.getItemStack().getTooltip(mc.thePlayer, false), mouseX, mouseY, (font == null ? fontRendererObj : font));
                    GlStateManager.popMatrix();
                }
            }
        }

        /**
         * GuiSlot#drawScreen: x of slot
         */
        private int getLeftX() {
            return this.left + this.width / 2 - this.getListWidth() / 2 + 2;
        }
    }

    public enum Column {
        ITEM_NAME("Item", 22),
        ITEM_AMOUNT("Amount", 200),
        PRICE_EACH("Price", 260),
        PRICE_SUM("Value", 330);

        private final int xOffset;
        private final String name;

        Column(String name, int xOffset) {
            this.name = "" + EnumChatFormatting.BOLD + EnumChatFormatting.UNDERLINE + name;
            this.xOffset = xOffset;
        }

        public String getName() {
            return name;
        }

        public int getXOffset() {
            return xOffset;
        }
    }
}
