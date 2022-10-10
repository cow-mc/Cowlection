package de.cowtipper.cowlection.chesttracker;

import com.google.common.collect.Lists;
import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.chesttracker.data.ItemData;
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
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.*;

public class ChestOverviewGui extends GuiScreen {
    private static final String SEARCH_QUERY_PLACE_HOLDER = "Search for...";

    private ItemOverview itemOverview;
    private List<GuiTooltip> guiTooltips;
    private GuiTextField fieldSearchQuery;
    private String searchQuery;
    private boolean isPlaceholderSearchQuery;
    private GuiButton btnClose;
    private GuiButton btnHelp;
    private GuiButton btnUpdatePrices;
    private GuiButton btnCopy;
    private GuiCheckBox chkShowNoPriceItems;
    private GuiCheckBox chkShowLowestBinItems;
    private GuiCheckBox chkShowBazaarItems;
    private GuiButton btnBazaarInstantOrOffer;
    private boolean useBazaarInstantSellPrices;
    private GuiCheckBox chkShowNpcItems;
    private AbortableRunnable updatePrices;
    private final String screenTitle;
    private final Cowlection main;

    public ChestOverviewGui(Cowlection main) {
        this.screenTitle = Cowlection.MODNAME + " Chest Analyzer";
        this.main = main;
        // clear wanted item chest highlighting
        main.getChestTracker().getChestsWithWantedItem().clear();
    }

    @Override
    public void initGui() {
        this.guiTooltips = new ArrayList<>();
        // close
        this.buttonList.add(this.btnClose = new GuiButtonExt(1, this.width - 25, 3, 20, 20, EnumChatFormatting.RED + "X"));
        addTooltip(btnClose, Arrays.asList(EnumChatFormatting.RED + "Close interface", "" + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + "Hint:" + EnumChatFormatting.RESET + " alternatively press ESC"));
        // help
        this.buttonList.add(this.btnHelp = new GuiButtonExt(2, this.width - 47, 3, 20, 20, "?"));
        addTooltip(btnHelp, Arrays.asList("" + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + screenTitle,
                EnumChatFormatting.GRAY + "You can set the default settings via " + EnumChatFormatting.YELLOW + "/moo config chest",
                "",
                EnumChatFormatting.WHITE + "  ‣ " + EnumChatFormatting.GOLD + "double click" + EnumChatFormatting.WHITE + ": highlight chests with selected item; hover over the chat message to see chest coords",
                EnumChatFormatting.WHITE + "  ‣ " + EnumChatFormatting.GOLD + "right click" + EnumChatFormatting.WHITE + ": exclude item from sell value calculation"));
        // update prices
        this.buttonList.add(this.btnUpdatePrices = new GuiButton(20, this.width - 145, 5, 90, 16, "⟳ Update prices"));
        addTooltip(btnUpdatePrices, Arrays.asList(EnumChatFormatting.YELLOW + "‣ Get latest " + EnumChatFormatting.GOLD + "Bazaar prices " + EnumChatFormatting.YELLOW + "from Hypixel API", EnumChatFormatting.WHITE + "   (only once per minute)",
                EnumChatFormatting.YELLOW + "‣ Get latest " + EnumChatFormatting.GOLD + "lowest BINs " + EnumChatFormatting.YELLOW + "from Moulberry's API", EnumChatFormatting.WHITE + "   (only once every 5 minutes)",
                EnumChatFormatting.YELLOW + "‣ Get latest " + EnumChatFormatting.GOLD + "NPC sell prices " + EnumChatFormatting.YELLOW + "from Hypixel API", EnumChatFormatting.WHITE + "   (only once every 15 minutes)"));
        // copy to clipboard
        this.buttonList.add(this.btnCopy = new GuiButton(21, this.width - 222, 5, 74, 16, "⎘ Copy data"));
        addTooltip(btnCopy, Collections.singletonList(EnumChatFormatting.YELLOW + "Copied data can be pasted into e.g. Google Spreadsheets"));
        // input: search
        this.fieldSearchQuery = new GuiTextField(23, this.fontRendererObj, this.width - 330, 6, 100, 15);
        addTooltip(fieldSearchQuery, Collections.singletonList(EnumChatFormatting.YELLOW + "Search by item name and item id"));
        this.fieldSearchQuery.setMaxStringLength(42);
        this.isPlaceholderSearchQuery = StringUtils.isEmpty(searchQuery) || SEARCH_QUERY_PLACE_HOLDER.equals(searchQuery);
        this.fieldSearchQuery.setText(isPlaceholderSearchQuery ? SEARCH_QUERY_PLACE_HOLDER : searchQuery);

        // toggle: use insta-sell or sell offer prices
        this.buttonList.add(this.btnBazaarInstantOrOffer = new GuiButton(15, this.width - 78, this.height - 51, 75, 12, MooConfig.useInstantSellBazaarPrices() ? "via Insta-Sell" : "via Sell Offer"));
        addTooltip(btnBazaarInstantOrOffer, Collections.singletonList(EnumChatFormatting.WHITE + "Bazaar items: " + EnumChatFormatting.YELLOW + "Use " + EnumChatFormatting.GOLD + "Instant-Sell " + EnumChatFormatting.YELLOW + "or " + EnumChatFormatting.GOLD + "Sell Offer" + EnumChatFormatting.YELLOW + " prices?"));
        // checkbox: show/hide Bazaar items
        this.buttonList.add(this.chkShowBazaarItems = new GuiCheckBox(10, this.width - 162, this.height - 50, " Bazaar items", MooConfig.chestAnalyzerShowBazaarItems));
        addTooltip(chkShowBazaarItems, Collections.singletonList(EnumChatFormatting.YELLOW + "Should items with a " + EnumChatFormatting.GOLD + "Bazaar " + EnumChatFormatting.YELLOW + "price be displayed?"));
        // checkbox: show/hide lowest BIN items
        this.buttonList.add(this.chkShowLowestBinItems = new GuiCheckBox(11, this.width - 162, this.height - 39, " lowest BIN items", MooConfig.chestAnalyzerShowLowestBinItems));
        addTooltip(chkShowLowestBinItems, Collections.singletonList(EnumChatFormatting.YELLOW + "Should items with a " + EnumChatFormatting.GOLD + "lowest BIN " + EnumChatFormatting.YELLOW + "price be displayed?"));
        // checkbox: show/hide NPC items
        this.buttonList.add(this.chkShowNpcItems = new GuiCheckBox(12, this.width - 162, this.height - 28, " NPC items", MooConfig.chestAnalyzerShowNpcItems));
        addTooltip(chkShowNpcItems, Lists.newArrayList(EnumChatFormatting.YELLOW + "Should items with an " + EnumChatFormatting.GOLD + "NPC sell " + EnumChatFormatting.YELLOW + "price be displayed?",
                EnumChatFormatting.RED + "(NPC sell price is only used if an item has neither a Bazaar nor lowest BIN price, or if one of them is hidden)"));
        // checkbox: show/hide items without a price
        this.buttonList.add(this.chkShowNoPriceItems = new GuiCheckBox(13, this.width - 162, this.height - 15, " items without price", MooConfig.chestAnalyzerShowNoPriceItems));
        addTooltip(chkShowNoPriceItems, Collections.singletonList(EnumChatFormatting.YELLOW + "Should items " + EnumChatFormatting.GOLD + "without " + EnumChatFormatting.YELLOW + "a Bazaar, BIN, or NPC price be displayed?"));
        // main item gui
        this.itemOverview = new ItemOverview();
    }

    private <T extends Gui> void addTooltip(T field, List<String> tooltip) {
        GuiTooltip guiTooltip = new GuiTooltip(field, tooltip);
        this.guiTooltips.add(guiTooltip);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        fieldSearchQuery.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        btnUpdatePrices.enabled = updatePrices == null && main.getChestTracker().allowAnyPriceUpdate();
        itemOverview.drawScreen(mouseX, mouseY, partialTicks);
        this.drawString(this.fontRendererObj, this.screenTitle, itemOverview.getLeftX(), 10, 0xFFCC00);
        this.fieldSearchQuery.drawTextBox();
        // left of checkboxes: price type indicators
        this.drawString(this.fontRendererObj, ItemData.PriceType.BAZAAR.getIndicator(), chkShowBazaarItems.xPosition - 5 - this.fontRendererObj.getStringWidth(ItemData.PriceType.BAZAAR.getIndicator()), chkShowBazaarItems.yPosition + 2, 0x666666);
        this.drawString(this.fontRendererObj, ItemData.PriceType.LOWEST_BIN.getIndicator(), chkShowLowestBinItems.xPosition - 5 - this.fontRendererObj.getStringWidth(ItemData.PriceType.LOWEST_BIN.getIndicator()), chkShowLowestBinItems.yPosition + 2, 0x666666);
        this.drawString(this.fontRendererObj, ItemData.PriceType.NPC_SELL.getIndicator(), chkShowNpcItems.xPosition - 5 - this.fontRendererObj.getStringWidth(ItemData.PriceType.NPC_SELL.getIndicator()), chkShowNpcItems.yPosition + 2, 0x666666);
        this.drawString(this.fontRendererObj, ItemData.PriceType.NONE.getIndicator(), chkShowNoPriceItems.xPosition - 5 - this.fontRendererObj.getStringWidth(ItemData.PriceType.NONE.getIndicator()), chkShowNoPriceItems.yPosition + 2, 0x666666);

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
            } else if (button == btnHelp) {
                mc.displayGuiScreen(new GuiChat("/moo config chest"));
            } else if (button == btnUpdatePrices) {
                btnUpdatePrices.enabled = false;
                EnumSet<ItemData.PriceType> updating = this.main.getChestTracker().refreshPriceCache();
                if (updating.size() > 0) {
                    updatePrices = new AbortableRunnable() {
                        private int retries = 20 * 20; // retry for up to 20 seconds
                        private final long previousBazaarUpdate = ChestTracker.lastBazaarUpdate;
                        private final long previousLowestBinsUpdate = ChestTracker.lastLowestBinsUpdate;
                        private final long previousNpcSellUpdate = ChestTracker.lastNpcSellUpdate;

                        @SubscribeEvent
                        public void onTickCheckPriceDataUpdated(TickEvent.ClientTickEvent e) {
                            if (!stopped && e.phase == TickEvent.Phase.END) {
                                if (Minecraft.getMinecraft().theWorld == null || retries <= 0) {
                                    // already stopped; or world gone, probably disconnected; or no retries left (took too long [20 seconds not enough?] or is not on SkyBlock): stop!
                                    stop();
                                    return;
                                }
                                retries--;
                                if (updating.contains(ItemData.PriceType.BAZAAR) && previousBazaarUpdate == ChestTracker.lastBazaarUpdate
                                        || updating.contains(ItemData.PriceType.LOWEST_BIN) && previousLowestBinsUpdate == ChestTracker.lastLowestBinsUpdate
                                        || updating.contains(ItemData.PriceType.NPC_SELL) && previousNpcSellUpdate == ChestTracker.lastNpcSellUpdate) {
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
            } else if (button == chkShowNoPriceItems || button == chkShowNpcItems || button == chkShowLowestBinItems || button == chkShowBazaarItems) {
                this.itemOverview.reloadItemData();
            } else if (button == btnCopy) {
                StringBuilder allItemData = new StringBuilder("Item\tItem (formatted)\tAmount\tPrice (instant-sell)\tValue (instant-sell)\tPrice (sell offer)\tValue (sell offer)\tPrice (lowest BIN)\tValue (lowest BIN)\tPrice (NPC sell)\tValue(NPC sell)");
                for (ItemData itemData : itemOverview.itemDataHolder) {
                    allItemData.append(itemData.toCopyableFormat());
                }
                allItemData.append("\n\n").append("Bazaar value (instant-sell):\t").append(itemOverview.summedValueInstaSell)
                        .append("\n").append("Bazaar value (sell offer):\t").append(itemOverview.summedValueSellOffer)
                        .append("\n").append("Auction House value (lowest BINs):\t").append(itemOverview.summedValueLowestBins)
                        .append("\n").append("NPC sell value (NPC sell):\t").append(itemOverview.summedValueNpcSell);
                GuiScreen.setClipboardString(allItemData.toString());
            } else if (button == btnBazaarInstantOrOffer) {
                this.btnBazaarInstantOrOffer.displayString = this.useBazaarInstantSellPrices ? "via Sell Offer" : "via Insta-Sell";
                this.useBazaarInstantSellPrices = !this.useBazaarInstantSellPrices;
                this.itemOverview.reloadItemData();
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        if (this.fieldSearchQuery.textboxKeyTyped(typedChar, keyCode)) {
            searchQuery = this.fieldSearchQuery.getText();
            isPlaceholderSearchQuery = SEARCH_QUERY_PLACE_HOLDER.equals(searchQuery);
            itemOverview.reloadItemData();
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
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.fieldSearchQuery.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.fieldSearchQuery.isFocused() && this.isPlaceholderSearchQuery) {
            this.fieldSearchQuery.setText("");
        }
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
        private long summedValueNpcSell;
        private long summedTotalValue;
        private boolean showBazaarItems;
        private boolean showLowestBinItems;
        private boolean showNpcItems;

        ItemOverview() {
            super(ChestOverviewGui.this.mc, ChestOverviewGui.this.width, ChestOverviewGui.this.height, 26, ChestOverviewGui.this.height - 54, 16);
            this.setShowSelectionBox(false);
            // space above first entry for control buttons
            int headerPadding = 20;
            this.setHasListHeader(true, headerPadding);

            reloadItemData();
        }

        private void reloadItemData() {
            showBazaarItems = ChestOverviewGui.this.chkShowBazaarItems.isChecked();
            showLowestBinItems = ChestOverviewGui.this.chkShowLowestBinItems.isChecked();
            showNpcItems = ChestOverviewGui.this.chkShowNpcItems.isChecked();

            EnumSet<ItemData.PriceType> visiblePriceTypes = EnumSet.of(
                    showBazaarItems ? ItemData.PriceType.BAZAAR : ItemData.PriceType.NONE,
                    showLowestBinItems ? ItemData.PriceType.LOWEST_BIN : ItemData.PriceType.NONE,
                    showNpcItems ? ItemData.PriceType.NPC_SELL : ItemData.PriceType.NONE
            );

            boolean useInstantSellPrices = ChestOverviewGui.this.useBazaarInstantSellPrices;
            itemDataHolder = main.getChestTracker().getAnalysisResult((isPlaceholderSearchQuery ? null : ChestOverviewGui.this.searchQuery), orderBy, orderDesc, visiblePriceTypes, useInstantSellPrices);
            summedValueInstaSell = 0;
            summedValueSellOffer = 0;
            summedValueLowestBins = 0;
            summedValueNpcSell = 0;
            summedTotalValue = 0;
            boolean showNoPriceItems = ChestOverviewGui.this.chkShowNoPriceItems.isChecked();

            for (Iterator<ItemData> iterator = itemDataHolder.iterator(); iterator.hasNext(); ) {
                ItemData itemData = iterator.next();
                boolean isVisibleItem = !itemData.isHidden();
                switch (itemData.getPriceType()) {
                    case BAZAAR:
                        if (isVisibleItem) {
                            summedValueInstaSell += itemData.getBazaarInstantSellValue();
                            summedValueSellOffer += itemData.getBazaarSellOfferValue();
                        }
                        if (showBazaarItems) {
                            continue;
                        }
                        break;
                    case LOWEST_BIN:
                        if (isVisibleItem) {
                            summedValueLowestBins += itemData.getLowestBinValue();
                        }
                        if (showLowestBinItems) {
                            continue;
                        }
                        break;
                    case NPC_SELL:
                        if (isVisibleItem) {
                            summedValueNpcSell += itemData.getNpcSellValue();
                        }
                        if (showNpcItems) {
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
            if (showNpcItems) {
                summedTotalValue += summedValueNpcSell;
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
            if (!isDoubleClick) {
                return;
            }
            ItemData itemData = itemDataHolder.get(slotIndex);
            if (itemData != null) {
                this.mc.displayGuiScreen(null);
                main.getChestTracker().markChestsWithWantedItem(itemData.getKey(), itemData.getAmount(), itemData.getName());
            }
        }

        @Override
        public void handleMouseInput() {
            super.handleMouseInput();
            if (Mouse.getEventButton() == /* right click */ 1 && Mouse.getEventButtonState() && this.isMouseYWithinSlotBounds(this.mouseY)) {
                int slotLeft = (this.width - this.getListWidth()) / 2;
                int slotRight = (this.width + this.getListWidth()) / 2;
                int k = this.mouseY - this.top - this.headerPadding + (int) this.amountScrolled - 4;
                int clickedSlot = k / this.slotHeight;

                if (clickedSlot < this.getSize() && this.mouseX >= slotLeft && this.mouseX <= slotRight && clickedSlot >= 0 && k >= 0) {
                    // right-clicked a slot
                    ItemData itemData = itemDataHolder.get(clickedSlot);
                    if (itemData != null) {
                        main.getChestTracker().toggleHiddenStateForItem(itemData.getKey());
                        this.reloadItemData();
                    }
                }
            }
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
        protected void drawSlot(int entryId, int x, int y, int z, int mouseXIn, int mouseYIn) {
            if (!isMouseYWithinSlotBounds(y + 5)) {
                // slot isn't visible anyways...
                return;
            }
            ItemData itemData = itemDataHolder.get(entryId);

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
            fontRenderer.drawStringWithShadow(itemName, itemNameXPos, y + 1, entryId % 2 == 0 ? 0xFFFFFF : 0x909090);
            fontRenderer.drawStringWithShadow(itemAmount, amountXPos, y + 1, entryId % 2 == 0 ? 0xFFFFFF : 0x909090);

            boolean useInstantSellPrices = ChestOverviewGui.this.useBazaarInstantSellPrices;
            double itemPrice = itemData.getPrice(useInstantSellPrices);
            String bazaarOrBinPrice = itemPrice > 0 ? Utils.formatDecimal(itemPrice) : EnumChatFormatting.DARK_GRAY + "?";
            fontRenderer.drawStringWithShadow(bazaarOrBinPrice, x + Column.PRICE_EACH.getXOffset() - fontRenderer.getStringWidth(bazaarOrBinPrice), y + 1, entryId % 2 == 0 ? 0xFFFFFF : 0x909090);

            double itemValue = itemData.getPriceSum(useInstantSellPrices);
            String bazaarOrBinValue = itemPrice > 0 ? Utils.formatNumber(itemValue) : EnumChatFormatting.DARK_GRAY + "?";
            fontRenderer.drawStringWithShadow(bazaarOrBinValue, x + Column.PRICE_SUM.getXOffset() - fontRenderer.getStringWidth(bazaarOrBinValue), y + 1, entryId % 2 == 0 ? 0xFFFFFF : 0x909090);

            String itemPriceType = itemData.getPriceType().getIndicator();
            fontRenderer.drawStringWithShadow(itemPriceType, x + Column.PRICE_TYPE.getXOffset() - fontRenderer.getStringWidth(itemPriceType), y + 1, entryId % 2 == 0 ? 0xFFFFFF : 0x909090);

            if (itemData.isHidden()) {
                Gui.drawRect(x, y - 3, x + getListWidth(), y - 3 + slotHeight, 0xdd444444);
            }
        }

        public void drawScreenPost(int mouseX, int mouseY) {
            GlStateManager.pushMatrix();
            float scaleFactor = 0.9f;
            GlStateManager.scale(scaleFactor, scaleFactor, 0);

            boolean useInstantSellPrices = ChestOverviewGui.this.useBazaarInstantSellPrices;

            drawSummedValue(scaleFactor, showBazaarItems && useInstantSellPrices, summedValueInstaSell, "Bazaar value (instant-sell prices)", 52);
            drawSummedValue(scaleFactor, showBazaarItems && !useInstantSellPrices, summedValueSellOffer, "Bazaar value (sell offer prices)", 43);
            drawSummedValue(scaleFactor, showLowestBinItems, summedValueLowestBins, "Auction House value (lowest BINs)", 34);
            drawSummedValue(scaleFactor, showNpcItems, summedValueNpcSell, "NPC value (NPC sell prices)", 25);

            String estimatedTotalSellValue = "estimated sell value ("
                    + (showBazaarItems ? (useInstantSellPrices ? "insta" : "offer") : "")
                    + (showLowestBinItems ? (showBazaarItems ? "+" : "") + "BIN" : "")
                    + (showNpcItems ? (showBazaarItems || showLowestBinItems ? "+" : "") + "NPC" : "") + ")";
            int estimatedTotalEndX = drawSummedValue(scaleFactor, (showBazaarItems || showLowestBinItems || showNpcItems), summedTotalValue, estimatedTotalSellValue, 11);

            drawRect((int) (3 / scaleFactor), (int) ((ChestOverviewGui.this.height - 15) / scaleFactor), (int) ((estimatedTotalEndX + 2) / scaleFactor), (int) ((ChestOverviewGui.this.height - 14) / scaleFactor), 0xff555555);
            GlStateManager.popMatrix();

            if (isMouseYWithinSlotBounds(mouseY)) {
                int slotIndex = this.getSlotIndexFromScreenCoords(mouseX, mouseY);

                if (slotIndex >= 0) {
                    // mouse is over a slot: maybe draw item tooltip
                    int xMin = getLeftX();
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

        private int drawSummedValue(float scaleFactor, boolean isPriceTypeEnabled, long summedValue, String valueType, int yOffset) {
            String valueText = (isPriceTypeEnabled ? EnumChatFormatting.WHITE : EnumChatFormatting.DARK_GRAY)
                    + "∑ " + valueType + ": "
                    + (summedValue > 0 ? Utils.formatNumber(summedValue) : EnumChatFormatting.DARK_GRAY + "-");
            return ChestOverviewGui.this.fontRendererObj.drawStringWithShadow(valueText,
                    Math.max(2, (getLeftX() + 175 - ChestOverviewGui.this.fontRendererObj.getStringWidth("∑ " + valueType + ": ")) / scaleFactor),
                    (ChestOverviewGui.this.height - yOffset) / scaleFactor, 0xdddddd);
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
        PRICE_EACH("Price", 275),
        PRICE_SUM("Value", 360),
        PRICE_TYPE("Type", 410);

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
