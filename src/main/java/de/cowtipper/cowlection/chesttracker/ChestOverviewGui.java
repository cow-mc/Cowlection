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
    private GuiButton btnUpdatePrices;
    private GuiButton btnCopy;
    private GuiCheckBox chkShowNoPriceItems;
    private GuiCheckBox chkShowLowestBinItems;
    private GuiCheckBox chkShowBazaarItems;
    private GuiButton btnBazaarInstantOrOffer;
    private boolean useBazaarInstantSellPrices;
    private AbortableRunnable updatePrices;
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
        // update prices
        this.buttonList.add(this.btnUpdatePrices = new GuiButton(20, this.width - 125, 5, 90, 16, "⟳ Update prices"));
        addTooltip(btnUpdatePrices, Arrays.asList(EnumChatFormatting.YELLOW + "‣ Get latest Bazaar prices from Hypixel API", EnumChatFormatting.WHITE + "   (only once per minute)",
                EnumChatFormatting.YELLOW + "‣ Get latest lowest BINs from Moulberry's API", EnumChatFormatting.WHITE + "   (only once every 5 minutes)"));
        // copy to clipboard
        this.buttonList.add(this.btnCopy = new GuiButton(21, this.width - 240, 5, 110, 16, "⎘ Copy to clipboard"));
        addTooltip(btnCopy, Collections.singletonList(EnumChatFormatting.YELLOW + "Copied data can be pasted into e.g. Google Spreadsheets"));

        // toggle: use insta-sell or sell offer prices
        this.buttonList.add(this.btnBazaarInstantOrOffer = new GuiButton(15, this.width - 165, this.height - 52, 130, 14, MooConfig.useInstantSellBazaarPrices() ? "Use Instant-Sell prices" : "Use Sell Offer prices"));
        addTooltip(btnBazaarInstantOrOffer, Collections.singletonList(EnumChatFormatting.YELLOW + "Use " + EnumChatFormatting.GOLD + "Instant-Sell " + EnumChatFormatting.YELLOW + "or " + EnumChatFormatting.GOLD + "Sell Offer" + EnumChatFormatting.YELLOW + " prices?"));
        // checkbox: show/hide Bazaar items
        this.buttonList.add(this.chkShowBazaarItems = new GuiCheckBox(10, this.width - 162, this.height - 36, " Show Bazaar items", MooConfig.chestAnalyzerShowBazaarItems));
        addTooltip(chkShowBazaarItems, Collections.singletonList(EnumChatFormatting.YELLOW + "Should items with a " + EnumChatFormatting.GOLD + "Bazaar " + EnumChatFormatting.YELLOW + "price be displayed?"));
        // checkbox: show/hide lowest BIN items
        this.buttonList.add(this.chkShowLowestBinItems = new GuiCheckBox(10, this.width - 162, this.height - 25, " Show lowest BIN items", MooConfig.chestAnalyzerShowLowestBinItems));
        addTooltip(chkShowLowestBinItems, Collections.singletonList(EnumChatFormatting.YELLOW + "Should items with a " + EnumChatFormatting.GOLD + "lowest BIN " + EnumChatFormatting.YELLOW + "price be displayed?"));
        // checkbox: show/hide items without a price
        this.buttonList.add(this.chkShowNoPriceItems = new GuiCheckBox(10, this.width - 162, this.height - 14, " Show items without price", MooConfig.chestAnalyzerShowNoPriceItems));
        addTooltip(chkShowNoPriceItems, Collections.singletonList(EnumChatFormatting.YELLOW + "Should items " + EnumChatFormatting.GOLD + "without " + EnumChatFormatting.YELLOW + "a Bazaar or BIN price be displayed?"));
        // main item gui
        this.itemOverview = new ItemOverview();
    }

    private <T extends Gui> void addTooltip(T field, List<String> tooltip) {
        GuiTooltip guiTooltip = new GuiTooltip(field, tooltip);
        this.guiTooltips.add(guiTooltip);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        btnUpdatePrices.enabled = updatePrices == null && (main.getChestTracker().allowUpdateBazaar() || main.getChestTracker().allowUpdateLowestBins());
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
            } else if (button == btnUpdatePrices) {
                btnUpdatePrices.enabled = false;
                EnumSet<ChestTracker.Updating> updating = this.main.getChestTracker().refreshPriceCache();
                if (updating.size() > 1) {
                    updatePrices = new AbortableRunnable() {
                        private int retries = 20 * 20; // retry for up to 20 seconds
                        private final long previousBazaarUpdate = ChestTracker.lastBazaarUpdate;
                        private final long previousLowestBinsUpdate = ChestTracker.lastLowestBinsUpdate;

                        @SubscribeEvent
                        public void onTickCheckPriceDataUpdated(TickEvent.ClientTickEvent e) {
                            if (!stopped && e.phase == TickEvent.Phase.END) {
                                if (Minecraft.getMinecraft().theWorld == null || retries <= 0) {
                                    // already stopped; or world gone, probably disconnected; or no retries left (took too long [20 seconds not enough?] or is not on SkyBlock): stop!
                                    stop();
                                    return;
                                }
                                retries--;
                                if (updating.contains(ChestTracker.Updating.BAZAAR) && previousBazaarUpdate == ChestTracker.lastBazaarUpdate
                                        || updating.contains(ChestTracker.Updating.LOWEST_BINS) && previousLowestBinsUpdate == ChestTracker.lastLowestBinsUpdate) {
                                    // cache(s) have not updated yet, retry next tick
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
                                stopPriceUpdateChecker();
                            }
                        }

                        @Override
                        public void run() {
                            MinecraftForge.EVENT_BUS.register(this);
                        }
                    };
                    new TickDelay(updatePrices, 20); // 1 second delay + retrying for 20 seconds, making sure price data got updated
                }
            } else if (button == chkShowNoPriceItems || button == chkShowLowestBinItems || button == chkShowBazaarItems) {
                this.itemOverview.reloadItemData();
            } else if (button == btnCopy) {
                StringBuilder allItemData = new StringBuilder("Item\tItem (formatted)\tAmount\tPrice (instant-sell)\tValue (instant-sell)\tPrice (sell offer)\tValue (sell offer)\tPrice (lowest BIN)\tValue (lowest BIN)");
                for (ItemData itemData : itemOverview.itemDataHolder) {
                    allItemData.append(itemData.toCopyableFormat());
                }
                allItemData.append("\n\n").append("Bazaar value (instant-sell):\t").append(itemOverview.summedValueInstaSell)
                        .append("\n").append("Bazaar value (sell offer):\t").append(itemOverview.summedValueSellOffer)
                        .append("\n").append("Auction House value (lowest BINs):\t").append(itemOverview.summedValueLowestBins);
                GuiScreen.setClipboardString(allItemData.toString());
            } else if (button == btnBazaarInstantOrOffer) {
                this.btnBazaarInstantOrOffer.displayString = this.useBazaarInstantSellPrices ? "Use Sell Offer prices" : "Use Instant-Sell prices";
                this.useBazaarInstantSellPrices = !this.useBazaarInstantSellPrices;
                this.itemOverview.reloadItemData();
            }
        }
    }

    private void stopPriceUpdateChecker() {
        if (updatePrices != null) {
            // there is still a price update-checker running, stop it
            updatePrices.stop();
            updatePrices = null;
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
        private long summedValueInstaSell;
        private long summedValueSellOffer;
        private long summedValueLowestBins;
        private long summedTotalValue;
        private boolean showBazaarItems;
        private boolean showLowestBinItems;

        ItemOverview() {
            super(ChestOverviewGui.this.mc, ChestOverviewGui.this.width, ChestOverviewGui.this.height, 32, ChestOverviewGui.this.height - 54, 16);
            this.setShowSelectionBox(false);
            // space above first entry for control buttons
            int headerPadding = 20;
            this.setHasListHeader(true, headerPadding);

            reloadItemData();
        }

        private void reloadItemData() {
            boolean useInstantSellPrices = ChestOverviewGui.this.useBazaarInstantSellPrices;
            itemDataHolder = main.getChestTracker().getAnalysisResult(orderBy, orderDesc, useInstantSellPrices);
            summedValueInstaSell = 0;
            summedValueSellOffer = 0;
            summedValueLowestBins = 0;
            summedTotalValue = 0;
            showBazaarItems = ChestOverviewGui.this.chkShowBazaarItems.isChecked();
            showLowestBinItems = ChestOverviewGui.this.chkShowLowestBinItems.isChecked();
            boolean showNoPriceItems = ChestOverviewGui.this.chkShowNoPriceItems.isChecked();

            for (Iterator<ItemData> iterator = itemDataHolder.iterator(); iterator.hasNext(); ) {
                ItemData itemData = iterator.next();
                switch (itemData.getPriceType()) {
                    case BAZAAR:
                        summedValueInstaSell += itemData.getBazaarInstantSellValue();
                        summedValueSellOffer += itemData.getBazaarSellOfferValue();
                        if (showBazaarItems) {
                            continue;
                        }
                        break;
                    case LOWEST_BIN:
                        summedValueLowestBins += itemData.getLowestBinValue();
                        if (showLowestBinItems) {
                            continue;
                        }
                        break;
                    default: // case NONE:
                        if (showNoPriceItems) {
                            continue;
                        }
                        break;
                }
                // otherwise: hide item
                iterator.remove();
            }
            if (showLowestBinItems) {
                summedTotalValue = summedValueLowestBins;
            }
            if (showBazaarItems) {
                summedTotalValue += (useInstantSellPrices ? summedValueInstaSell : summedValueSellOffer);
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
                ChestOverviewGui.this.fontRendererObj.drawStringWithShadow(column.getName(), columnX, y + 2, 0xFFFFFF);
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
            fontRenderer.drawStringWithShadow(itemName, itemNameXPos, y + 1, entryID % 2 == 0 ? 0xFFFFFF : 0x909090);
            fontRenderer.drawStringWithShadow(itemAmount, amountXPos, y + 1, entryID % 2 == 0 ? 0xFFFFFF : 0x909090);

            boolean useInstantSellPrices = ChestOverviewGui.this.useBazaarInstantSellPrices;
            double itemPrice = itemData.getPrice(useInstantSellPrices);
            String bazaarOrBinPrice = itemPrice > 0 ? Utils.formatDecimal(itemPrice) : EnumChatFormatting.DARK_GRAY + "?";
            fontRenderer.drawStringWithShadow(bazaarOrBinPrice, x + Column.PRICE_EACH.getXOffset() - fontRenderer.getStringWidth(bazaarOrBinPrice), y + 1, entryID % 2 == 0 ? 0xFFFFFF : 0x909090);

            double itemValue = itemData.getPriceSum(useInstantSellPrices);
            String bazaarOrBinValue = itemPrice > 0 ? Utils.formatNumber(itemValue) : EnumChatFormatting.DARK_GRAY + "?";
            fontRenderer.drawStringWithShadow(bazaarOrBinValue, x + Column.PRICE_SUM.getXOffset() - fontRenderer.getStringWidth(bazaarOrBinValue), y + 1, entryID % 2 == 0 ? 0xFFFFFF : 0x909090);
        }

        public void drawScreenPost(int mouseX, int mouseY) {
            int xMin = getLeftX();
            FontRenderer fontRenderer = ChestOverviewGui.this.fontRendererObj;

            boolean useInstantSellPrices = ChestOverviewGui.this.useBazaarInstantSellPrices;

            String bazaarValueInstantSell = ((showBazaarItems && useInstantSellPrices) ? EnumChatFormatting.WHITE : EnumChatFormatting.DARK_GRAY) + "∑ Bazaar value (instant-sell prices): " + (summedValueInstaSell > 0 ? Utils.formatNumber(summedValueInstaSell) : EnumChatFormatting.DARK_GRAY + "―"); // sum
            fontRenderer.drawStringWithShadow(bazaarValueInstantSell, xMin + 175 - fontRenderer.getStringWidth("∑ Bazaar value (instant-sell prices): "), ChestOverviewGui.this.height - 50, 0xdddddd);
            String bazaarValueSellOffer = ((showBazaarItems && !useInstantSellPrices) ? EnumChatFormatting.WHITE : EnumChatFormatting.DARK_GRAY) + "∑ Bazaar value (sell offer prices): " + (summedValueSellOffer > 0 ? Utils.formatNumber(summedValueSellOffer) : EnumChatFormatting.DARK_GRAY + "―"); // sum
            fontRenderer.drawStringWithShadow(bazaarValueSellOffer, xMin + 175 - fontRenderer.getStringWidth("∑ Bazaar value (sell offer prices): "), ChestOverviewGui.this.height - 39, 0xdddddd);
            String lowestBinValue = ((showLowestBinItems) ? EnumChatFormatting.WHITE : EnumChatFormatting.DARK_GRAY) + "∑ Auction House value (lowest BINs): " + (summedValueLowestBins > 0 ? Utils.formatNumber(summedValueLowestBins) : EnumChatFormatting.DARK_GRAY + "―"); // sum
            fontRenderer.drawStringWithShadow(lowestBinValue, xMin + 175 - fontRenderer.getStringWidth("∑ Auction House value (lowest BINs): "), ChestOverviewGui.this.height - 28, 0xdddddd);
            String estimatedSellValue = ((showBazaarItems || showLowestBinItems) ? EnumChatFormatting.WHITE : EnumChatFormatting.DARK_GRAY)
                    + "∑ estimated sell value ("
                    + (showBazaarItems ? (useInstantSellPrices ? "insta" : "offer") : "")
                    + (showLowestBinItems ? (showBazaarItems ? "+" : "") + "BIN" : "")
                    + "): ";
            String totalValue = estimatedSellValue + (summedTotalValue > 0 ? EnumChatFormatting.WHITE + Utils.formatNumber(summedTotalValue) : EnumChatFormatting.DARK_GRAY + "―"); // sum
            int estimatedTotalEndX = fontRenderer.drawStringWithShadow(totalValue, xMin + 175 - fontRenderer.getStringWidth(estimatedSellValue), ChestOverviewGui.this.height - 13, 0xdddddd);

            drawRect(3, ChestOverviewGui.this.height - 16, estimatedTotalEndX + 2, ChestOverviewGui.this.height - 15, 0xff555555);

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
                    ChestOverviewGui.this.drawHoveringText(itemData.getItemStack().getTooltip(mc.thePlayer, false), mouseX, mouseY, (font == null ? ChestOverviewGui.this.fontRendererObj : font));
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
