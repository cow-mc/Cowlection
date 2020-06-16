package eu.olli.cowmoonication.search;

import com.google.common.base.Joiner;
import eu.olli.cowmoonication.config.MooConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.client.GuiScrollingList;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executors;

public class GuiSearch extends GuiScreen {
    private static final String SEARCH_QUERY_PLACE_HOLDER = "Search for...";
    // data
    private String searchQuery;
    private boolean matchCase;
    private boolean removeFormatting;
    /**
     * Cached results are required after resizing the client
     */
    private List<String> searchResults;
    private LocalDate dateStart;
    private LocalDate dateEnd;

    // gui elements
    private GuiButton buttonSearch;
    private GuiButton buttonClose;
    private GuiCheckBox checkboxMatchCase;
    private GuiCheckBox checkboxRemoveFormatting;
    private GuiTextField fieldSearchQuery;
    private GuiDateField fieldDateStart;
    private GuiDateField fieldDateEnd;
    private SearchResults guiSearchResults;
    private List<GuiTooltip> guiTooltips;
    private boolean isSearchInProgress;

    public GuiSearch() {
        this.searchQuery = SEARCH_QUERY_PLACE_HOLDER;
        this.matchCase = false;
        this.searchResults = new ArrayList<>();
        this.dateStart = MooConfig.calculateStartDate();
        this.dateEnd = LocalDate.now();
    }

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    @Override
    public void initGui() {
        this.guiTooltips = new ArrayList<>();

        this.fieldSearchQuery = new GuiTextField(42, this.fontRendererObj, this.width / 2 - 100, 15, 200, 20);
        this.fieldSearchQuery.setMaxStringLength(255);
        this.fieldSearchQuery.setText(searchQuery);
        if (SEARCH_QUERY_PLACE_HOLDER.equals(searchQuery)) {
            this.fieldSearchQuery.setFocused(true);
            this.fieldSearchQuery.setSelectionPos(0);
        }

        // date fields
        this.fieldDateStart = new GuiDateField(50, this.fontRendererObj, this.width / 2 + 110, 15, 70, 15);
        this.fieldDateStart.setText(dateStart.toString());
        addTooltip(fieldDateStart, Arrays.asList(EnumChatFormatting.YELLOW + "Start date", "" + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + "Format: " + EnumChatFormatting.RESET + "year-month-day"));

        this.fieldDateEnd = new GuiDateField(51, this.fontRendererObj, this.width / 2 + 110, 35, 70, 15);
        this.fieldDateEnd.setText(dateEnd.toString());
        addTooltip(fieldDateEnd, Arrays.asList(EnumChatFormatting.YELLOW + "End date", "" + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + "Format: " + EnumChatFormatting.RESET + "year-month-day"));

        // buttons
        this.buttonList.add(this.buttonClose = new GuiButtonExt(0, this.width - 25, 3, 22, 20, EnumChatFormatting.RED + "X"));
        addTooltip(buttonClose, Arrays.asList(EnumChatFormatting.RED + "Close search interface", "" + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + "Hint:" + EnumChatFormatting.RESET + " alternatively press ESC"));

        this.buttonList.add(this.checkboxMatchCase = new GuiCheckBox(1, this.width / 2 - 100, 40, " Match case", matchCase));
        addTooltip(checkboxMatchCase, Collections.singletonList(EnumChatFormatting.YELLOW + "Should the search be " + EnumChatFormatting.GOLD + "case-sensitive" + EnumChatFormatting.YELLOW + "?"));
        this.buttonList.add(this.checkboxRemoveFormatting = new GuiCheckBox(1, this.width / 2 - 100, 50, " Remove formatting", removeFormatting));
        addTooltip(checkboxRemoveFormatting, Collections.singletonList(EnumChatFormatting.YELLOW + "Should " + EnumChatFormatting.GOLD + "formatting " + EnumChatFormatting.YELLOW + "and " + EnumChatFormatting.GOLD + "color codes " + EnumChatFormatting.YELLOW + "be " + EnumChatFormatting.GOLD + "removed " + EnumChatFormatting.YELLOW + "from the search results?"));
        this.buttonList.add(this.buttonSearch = new GuiButtonExt(100, this.width / 2 + 40, 40, 60, 20, "Search"));

        this.guiSearchResults = new SearchResults(70);
        this.guiSearchResults.setResults(searchResults);

        this.setIsSearchInProgress(isSearchInProgress);

        boolean isStartDateValid = fieldDateStart.validateDate();
        boolean isEndDateValid = fieldDateEnd.validateDate();
        this.buttonSearch.enabled = !isSearchInProgress && this.fieldSearchQuery.getText().trim().length() > 1 && !this.fieldSearchQuery.getText().startsWith(SEARCH_QUERY_PLACE_HOLDER) && isStartDateValid && isEndDateValid && !dateStart.isAfter(dateEnd);

        if (isStartDateValid && isEndDateValid && dateStart.isAfter(dateEnd)) {
            fieldDateStart.setTextColor(0xFFDD3333);
            fieldDateEnd.setTextColor(0xFFCC3333);
        }
    }

    private <T extends Gui> void addTooltip(T field, List<String> tooltip) {
        GuiTooltip guiTooltip = new GuiTooltip(field, tooltip);
        this.guiTooltips.add(guiTooltip);
    }

    @Override
    public void updateScreen() {
        fieldSearchQuery.updateCursorCounter();
        fieldDateStart.updateCursorCounter();
        fieldDateEnd.updateCursorCounter();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // allow clicks on 'close' button even while a search is in progress
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (isSearchInProgress) {
            // search in progress, abort
            return;
        }
        fieldSearchQuery.mouseClicked(mouseX, mouseY, mouseButton);
        fieldDateStart.mouseClicked(mouseX, mouseY, mouseButton);
        fieldDateEnd.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (isSearchInProgress && keyCode != Keyboard.KEY_ESCAPE) {
            // search in progress, don't process key typed - but allow escape to exit gui
            return;
        }
        if (dateStart.isBefore(dateEnd)) {
            fieldDateStart.setTextColor(0xFFFFFFFF);
            fieldDateEnd.setTextColor(0xFFFFFFFF);
        }
        if (keyCode == Keyboard.KEY_RETURN && this.fieldSearchQuery.isFocused()) {
            // perform search
            actionPerformed(buttonSearch);
        } else if (this.fieldSearchQuery.textboxKeyTyped(typedChar, keyCode)) {
            searchQuery = this.fieldSearchQuery.getText();
        } else if (this.fieldDateStart.textboxKeyTyped(typedChar, keyCode)) {
            if (fieldDateStart.validateDate()) {
                dateStart = fieldDateStart.getDate();
            }
        } else if (this.fieldDateEnd.textboxKeyTyped(typedChar, keyCode)) {
            if (fieldDateEnd.validateDate()) {
                dateEnd = fieldDateEnd.getDate();
            }
        } else if (GuiScreen.isKeyComboCtrlA(keyCode)) {
            // copy all search results
            String searchResults = guiSearchResults.getAllSearchResults();
            if (!searchResults.isEmpty()) {
                GuiScreen.setClipboardString(EnumChatFormatting.getTextWithoutFormattingCodes(searchResults));
            }
        } else if (GuiScreen.isKeyComboCtrlC(keyCode)) {
            // copy current selected entry
            String selectedSearchResult = guiSearchResults.getSelectedSearchResult();
            if (selectedSearchResult != null) {
                GuiScreen.setClipboardString(EnumChatFormatting.getTextWithoutFormattingCodes(selectedSearchResult));
            }
        } else {
            super.keyTyped(typedChar, keyCode);
        }

        boolean isStartDateValid = fieldDateStart.validateDate();
        boolean isEndDateValid = fieldDateEnd.validateDate();
        this.buttonSearch.enabled = !isSearchInProgress && searchQuery.trim().length() > 1 && !searchQuery.startsWith(SEARCH_QUERY_PLACE_HOLDER) && isStartDateValid && isEndDateValid && !dateStart.isAfter(dateEnd);

        if (isStartDateValid && isEndDateValid && dateStart.isAfter(dateEnd)) {
            fieldDateStart.setTextColor(0xFFDD3333);
            fieldDateEnd.setTextColor(0xFFCC3333);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, EnumChatFormatting.BOLD + "Minecraft Log Search", this.width / 2, 3, 0xFFFFFF);
        this.fieldSearchQuery.drawTextBox();
        this.fieldDateStart.drawTextBox();
        this.fieldDateEnd.drawTextBox();
        this.guiSearchResults.drawScreen(mouseX, mouseY, partialTicks);

        super.drawScreen(mouseX, mouseY, partialTicks);

        for (GuiTooltip guiTooltip : guiTooltips) {
            if (guiTooltip.checkHover(mouseX, mouseY)) {
                drawHoveringText(guiTooltip.getText(), mouseX, mouseY, 300);
                // only one tooltip can be displayed at a time: break!
                break;
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == this.buttonClose && button.enabled) {
            this.mc.setIngameFocus();
        }
        if (isSearchInProgress || !button.enabled) {
            return;
        }
        if (button == this.buttonSearch) {
            setIsSearchInProgress(true);

            Executors.newSingleThreadExecutor().execute(() -> {
                searchResults = new LogFilesSearcher().searchFor(this.fieldSearchQuery.getText(), checkboxMatchCase.isChecked(), checkboxRemoveFormatting.isChecked(), dateStart, dateEnd);
                if (searchResults.isEmpty()) {
                    searchResults.add(EnumChatFormatting.ITALIC + "No results");
                }
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    this.guiSearchResults.setResults(searchResults);
                    setIsSearchInProgress(false);
                });
            });
        } else if (button == checkboxMatchCase) {
            matchCase = checkboxMatchCase.isChecked();
        } else if (button == checkboxRemoveFormatting) {
            removeFormatting = checkboxRemoveFormatting.isChecked();
        }
    }

    private void setIsSearchInProgress(boolean isSearchInProgress) {
        this.isSearchInProgress = isSearchInProgress;
        buttonSearch.enabled = !isSearchInProgress;
        fieldSearchQuery.setEnabled(!isSearchInProgress);
        fieldDateStart.setEnabled(!isSearchInProgress);
        fieldDateEnd.setEnabled(!isSearchInProgress);
        checkboxRemoveFormatting.enabled = !isSearchInProgress;
        checkboxMatchCase.enabled = !isSearchInProgress;
        if (isSearchInProgress) {
            fieldSearchQuery.setFocused(false);
            fieldDateStart.setFocused(false);
            fieldDateEnd.setFocused(false);
            buttonSearch.displayString = EnumChatFormatting.ITALIC + "Searching";
            searchResults.clear();
            this.guiSearchResults.clearResults();
        } else {
            buttonSearch.displayString = "Search";
        }
    }

    private void drawHoveringText(List<String> textLines, int mouseX, int mouseY, int maxTextWidth) {
        if (ForgeVersion.getBuildVersion() < 1808) {
            // we're running a forge version from before 24 March 2016 (http://files.minecraftforge.net/maven/net/minecraftforge/forge/index_1.8.9.html for reference)
            // using mc built-in method
            drawHoveringText(textLines, mouseX, mouseY, fontRendererObj);
        } else {
            // we're on a newer forge version, so we can use the improved tooltip rendering added in 1.8.9-11.15.1.1808 (released 03/24/16 09:25 PM) in this pull request: https://github.com/MinecraftForge/MinecraftForge/pull/2649
            GuiUtils.drawHoveringText(textLines, mouseX, mouseY, width, height, maxTextWidth, fontRendererObj);
        }
    }

    /**
     * List gui element similar to GuiModList.Info
     */
    class SearchResults extends GuiScrollingList {
        private final String[] spinner = new String[]{"oooooo", "Oooooo", "oOoooo", "ooOooo", "oooOoo", "ooooOo", "oooooO"};
        private List<String> rawResults;
        private List<IChatComponent> slotsData;
        private NavigableMap<Integer, Integer> searchResultEntries;

        SearchResults(int marginTop) {
            super(GuiSearch.this.mc,
                    GuiSearch.this.width - 10, // 5 pixel margin each
                    GuiSearch.this.height - marginTop - 5,
                    marginTop, GuiSearch.this.height - 5,
                    5, 12,
                    GuiSearch.this.width,
                    GuiSearch.this.height);
            this.rawResults = Collections.emptyList();
            this.slotsData = Collections.emptyList();
            this.searchResultEntries = Collections.emptyNavigableMap();
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            super.drawScreen(mouseX, mouseY, partialTicks);
            if (isSearchInProgress) {
                // spinner taken from IProgressMeter and GuiAchievements#drawScreen
                GuiSearch.this.drawCenteredString(GuiSearch.this.fontRendererObj, "Searching for '" + GuiSearch.this.searchQuery + "'", GuiSearch.this.width / 2, GuiSearch.this.height / 2, 16777215);
                GuiSearch.this.drawCenteredString(GuiSearch.this.fontRendererObj, spinner[(int) (Minecraft.getSystemTime() / 150L % (long) spinner.length)], GuiSearch.this.width / 2, GuiSearch.this.height / 2 + GuiSearch.this.fontRendererObj.FONT_HEIGHT * 2, 16777215);
            }
        }

        @Override
        protected int getSize() {
            return slotsData.size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick) {
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
            if (Objects.equals(searchResultEntries.floorKey(selectedIndex), searchResultEntries.floorKey(slotIdx))) {
                // highlight all lines of selected entry
                drawRect(this.left, slotTop - 2, entryRight, slotTop + slotHeight - 2, 0x99000000);
            }
            IChatComponent slotData = slotsData.get(slotIdx);
            if (slotData != null) {
                GlStateManager.enableBlend();
                GuiSearch.this.fontRendererObj.drawStringWithShadow(slotData.getFormattedText(), this.left + 4, slotTop, 0xFFFFFF);
                GlStateManager.disableAlpha();
                GlStateManager.disableBlend();
            }
        }

        private void setResults(List<String> searchResult) {
            this.rawResults = searchResult;
            this.slotsData = resizeContent(searchResult);
        }

        private void clearResults() {
            this.rawResults = Collections.emptyList();
            this.slotsData = resizeContent(Collections.emptyList());
        }

        private List<IChatComponent> resizeContent(List<String> searchResults) {
            this.searchResultEntries = new TreeMap<>();
            List<IChatComponent> slotsData = new ArrayList<>();
            for (int searchResultIndex = 0; searchResultIndex < searchResults.size(); searchResultIndex++) {
                String searchResult = searchResults.get(searchResultIndex);

                searchResultEntries.put(slotsData.size(), searchResultIndex);
                IChatComponent chat = ForgeHooks.newChatWithLinks(searchResult, false);
                List<IChatComponent> multilineResult = GuiUtilRenderComponents.splitText(chat, this.listWidth - 8, GuiSearch.this.fontRendererObj, false, true);
                slotsData.addAll(multilineResult);
            }
            return slotsData;
        }

        String getSelectedSearchResult() {
            Map.Entry<Integer, Integer> selectedResultIndex = searchResultEntries.floorEntry(selectedIndex);
            return (selectedResultIndex != null && selectedResultIndex.getValue() < rawResults.size()) ? rawResults.get(selectedResultIndex.getValue()) : null;
        }

        String getAllSearchResults() {
            return Joiner.on('\n').join(rawResults);
        }
    }
}
