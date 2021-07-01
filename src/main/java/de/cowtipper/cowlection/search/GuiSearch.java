package de.cowtipper.cowlection.search;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.realmsclient.util.Pair;
import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.util.GuiHelper;
import de.cowtipper.cowlection.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.GuiScrollingList;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class GuiSearch extends GuiScreen {
    private static final String SEARCH_QUERY_PLACE_HOLDER = "Search for...";
    private final File mcLogOutputFile;
    /**
     * @see Executors#newCachedThreadPool()
     */
    private final ExecutorService executorService = new ThreadPoolExecutor(0, 1,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(), new ThreadFactoryBuilder().setNameFormat(Cowlection.MODID + "-logfilesearcher-%d").build());
    // data
    private boolean hasInitialSearchQuery;
    private String searchQuery;
    private boolean chatOnly;
    private boolean matchCase;
    private boolean removeFormatting;
    /**
     * Cached results are required after resizing the client
     */
    private List<LogEntry> searchResults;
    private LocalDate dateStart;
    private LocalDate dateEnd;

    // gui elements
    private GuiButton buttonSearch;
    private GuiButton buttonClose;
    private GuiButton buttonHelp;
    private GuiButton buttonSettings;
    private GuiCheckBox checkboxChatOnly;
    private GuiCheckBox checkboxMatchCase;
    private GuiCheckBox checkboxRemoveFormatting;
    private GuiTextField fieldSearchQuery;
    private GuiDateField fieldDateStart;
    private GuiDateField fieldDateEnd;
    private SearchResults guiSearchResults;
    private List<GuiTooltip> guiTooltips;
    private boolean isSearchInProgress;
    private String analyzedFiles;
    private String analyzedFilesWithHits;
    private String skippedFiles;
    private boolean areEntriesSearchResults;

    public GuiSearch(String initialSearchQuery) {
        this.mcLogOutputFile = new File(Cowlection.getInstance().getModOutDirectory(), "cowlection-mc-log-search-temp.txt");
        try {
            mcLogOutputFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (initialSearchQuery.length() > 0) {
            hasInitialSearchQuery = true;
            this.searchQuery = initialSearchQuery;
        } else {
            this.searchQuery = SEARCH_QUERY_PLACE_HOLDER;
        }
        this.searchResults = new ArrayList<>();
        this.dateStart = MooConfig.calculateStartDate();
        this.dateEnd = LocalDate.now();
        this.chatOnly = true;
    }

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    @Override
    public void initGui() {
        this.guiTooltips = new ArrayList<>();

        // recalculate start date
        this.dateStart = MooConfig.calculateStartDate();

        this.fieldSearchQuery = new GuiTextField(42, this.fontRendererObj, this.width / 2 - 100, 13, 200, 20);
        this.fieldSearchQuery.setMaxStringLength(255);
        this.fieldSearchQuery.setText(searchQuery);
        if (SEARCH_QUERY_PLACE_HOLDER.equals(searchQuery)) {
            this.fieldSearchQuery.setFocused(true);
            this.fieldSearchQuery.setSelectionPos(0);
        } else if (hasInitialSearchQuery) {
            this.fieldSearchQuery.setFocused(true);
            this.hasInitialSearchQuery = false;
        }

        // date field: start
        this.fieldDateStart = new GuiDateField(50, this.fontRendererObj, this.width / 2 + 110, 15, 70, 15);
        this.fieldDateStart.setText(dateStart.toString());
        addTooltip(fieldDateStart, Arrays.asList(EnumChatFormatting.YELLOW + "Start date", "" + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + "Format: " + EnumChatFormatting.RESET + "year-month-day"));
        // date field: end
        this.fieldDateEnd = new GuiDateField(51, this.fontRendererObj, this.width / 2 + 110, 35, 70, 15);
        this.fieldDateEnd.setText(dateEnd.toString());
        addTooltip(fieldDateEnd, Arrays.asList(EnumChatFormatting.YELLOW + "End date", "" + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + "Format: " + EnumChatFormatting.RESET + "year-month-day"));

        // close
        this.buttonList.add(this.buttonClose = new GuiButtonExt(0, this.width - 25, 3, 22, 20, EnumChatFormatting.RED + "X"));
        addTooltip(buttonClose, Arrays.asList(EnumChatFormatting.RED + "Close search interface", "" + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + "Hint:" + EnumChatFormatting.RESET + " alternatively press ESC"));
        // help
        this.buttonList.add(this.buttonHelp = new GuiButtonExt(1, this.width - 2 * 25, 3, 22, 20, "?"));
        addTooltip(buttonHelp, Collections.singletonList(EnumChatFormatting.YELLOW + "Show help"));
        // settings
        this.buttonList.add(this.buttonSettings = new GuiButtonExt(1, this.width - 3 * 25, 3, 22, 20, ""));
        addTooltip(buttonSettings, Collections.singletonList(EnumChatFormatting.YELLOW + "Open Settings"));

        // chatOnly
        this.buttonList.add(this.checkboxChatOnly = new GuiCheckBox(21, this.width / 2 - 100, 35, " Chatbox only", chatOnly));
        addTooltip(checkboxChatOnly, Collections.singletonList(EnumChatFormatting.YELLOW + "Should " + EnumChatFormatting.GOLD + "only " + EnumChatFormatting.YELLOW + "results that have " + EnumChatFormatting.GOLD + "appeared in the chat box " + EnumChatFormatting.YELLOW + "be displayed?\n"
                + EnumChatFormatting.GRAY + "For example, this " + EnumChatFormatting.WHITE + "excludes error messages" + EnumChatFormatting.GRAY + " but still " + EnumChatFormatting.WHITE + "includes messages sent by a server" + EnumChatFormatting.GRAY + "."));
        // matchCase
        this.buttonList.add(this.checkboxMatchCase = new GuiCheckBox(20, this.width / 2 - 100, 45, " Match case", matchCase));
        addTooltip(checkboxMatchCase, Collections.singletonList(EnumChatFormatting.YELLOW + "Should the search be " + EnumChatFormatting.GOLD + "case-sensitive" + EnumChatFormatting.YELLOW + "?"));
        // removeFormatting
        this.buttonList.add(this.checkboxRemoveFormatting = new GuiCheckBox(22, this.width / 2 - 100, 55, " Remove formatting", removeFormatting));
        addTooltip(checkboxRemoveFormatting, Collections.singletonList(EnumChatFormatting.YELLOW + "Should " + EnumChatFormatting.GOLD + "formatting " + EnumChatFormatting.YELLOW + "and " + EnumChatFormatting.GOLD + "color codes " + EnumChatFormatting.YELLOW + "be " + EnumChatFormatting.GOLD + "removed " + EnumChatFormatting.YELLOW + "from the search results?"));
        // search
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
                GuiScreen.setClipboardString(searchResults);
            }
        } else if (GuiScreen.isKeyComboCtrlC(keyCode)) {
            // copy current selected entry
            LogEntry selectedSearchResult = guiSearchResults.getSelectedSearchResult();
            if (selectedSearchResult != null) {
                GuiScreen.setClipboardString(EnumChatFormatting.getTextWithoutFormattingCodes(selectedSearchResult.getMessage()));
            }
        } else if (keyCode == Keyboard.KEY_C && isCtrlKeyDown() && isShiftKeyDown() && !isAltKeyDown()) {
            // copy current selected entry with formatting codes
            LogEntry selectedSearchResult = guiSearchResults.getSelectedSearchResult();
            if (selectedSearchResult != null) {
                GuiScreen.setClipboardString(selectedSearchResult.getMessage());
            }
        } else {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                guiSearchResults = null;
            }
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
        this.drawCenteredString(this.fontRendererObj, EnumChatFormatting.BOLD + "Minecraft Log Search", this.width / 2, 2, 0xFFFFFF);
        this.fieldSearchQuery.drawTextBox();
        this.fieldDateStart.drawTextBox();
        this.fieldDateEnd.drawTextBox();
        this.guiSearchResults.drawScreen(mouseX, mouseY, partialTicks);

        super.drawScreen(mouseX, mouseY, partialTicks);
        GuiHelper.drawSprite(this.width - 3 * 25 + 2, 3, 36, 18, 500);

        for (GuiTooltip guiTooltip : guiTooltips) {
            if (guiTooltip.checkHover(mouseX, mouseY)) {
                GuiHelper.drawHoveringText(guiTooltip.getText(), mouseX, mouseY, width, height, 300);
                // only one tooltip can be displayed at a time: break!
                break;
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == this.buttonClose && button.enabled) {
            guiSearchResults = null;
            this.mc.setIngameFocus();
        }
        if (isSearchInProgress || !button.enabled) {
            return;
        }
        if (button == this.buttonSearch) {
            setIsSearchInProgress(true);

            executorService.execute(() -> {
                try {
                    LogSearchResults searchResultsData = new LogFilesSearcher().searchFor(this.fieldSearchQuery.getText(), checkboxChatOnly.isChecked(), checkboxMatchCase.isChecked(), checkboxRemoveFormatting.isChecked(), dateStart, dateEnd);
                    this.searchResults = searchResultsData.getSortedSearchResults();
                    this.analyzedFiles = "Analyzed files: " + EnumChatFormatting.WHITE + searchResultsData.getAnalyzedFiles();
                    this.analyzedFilesWithHits = "Files with hits: " + EnumChatFormatting.WHITE + searchResultsData.getAnalyzedFilesWithHits();
                    this.skippedFiles = "Skipped files: " + EnumChatFormatting.WHITE + searchResultsData.getSkippedFiles();
                    if (this.searchResults.isEmpty()) {
                        this.searchResults.add(new LogEntry(EnumChatFormatting.ITALIC + "No results"));
                        areEntriesSearchResults = false;
                    } else {
                        areEntriesSearchResults = searchResultsData.getAnalyzedFiles() != 0;
                    }
                } catch (IOException e) {
                    if (e.getStackTrace().length > 0) {
                        searchResults.add(new LogEntry(StringUtils.replaceEach(ExceptionUtils.getStackTrace(e), new String[]{"\t", "\r\n"}, new String[]{"  ", "\n"})));
                        areEntriesSearchResults = false;
                    }
                }
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    this.guiSearchResults.setResults(this.searchResults);
                    setIsSearchInProgress(false);
                });
            });
        } else if (button == checkboxChatOnly) {
            chatOnly = checkboxChatOnly.isChecked();
        } else if (button == checkboxMatchCase) {
            matchCase = checkboxMatchCase.isChecked();
        } else if (button == checkboxRemoveFormatting) {
            removeFormatting = checkboxRemoveFormatting.isChecked();
        } else if (button == buttonHelp) {
            this.areEntriesSearchResults = false;
            this.searchResults.clear();
            this.searchResults.add(new LogEntry("" + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + "Initial setup/configuration " + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + "'Open Settings' (top right corner)"));
            this.searchResults.add(new LogEntry(EnumChatFormatting.GOLD + " 1) " + EnumChatFormatting.RESET + "Configure directories that should be scanned for log files (\"Directories with Minecraft log files\")"));
            this.searchResults.add(new LogEntry(EnumChatFormatting.GOLD + " 2) " + EnumChatFormatting.RESET + "Set default starting date (\"Start date for log file search\")"));
            this.searchResults.add(new LogEntry("      ‣ can be a number (e.g. \"3\" means \"start searching 3 months ago\")"));
            this.searchResults.add(new LogEntry("      ‣ or alternatively a fixed date (yyyy-mm-dd)"));
            this.searchResults.add(new LogEntry(EnumChatFormatting.GOLD + " 3) " + EnumChatFormatting.RESET + "optional: change the maximum allowed log file size to be searched, but note that each log file must be unzipped before it can be analyzed, which can make the log file search take significantly longer for large files"));
            this.searchResults.add(new LogEntry("" + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + "Performing a search " + EnumChatFormatting.GRAY + EnumChatFormatting.ITALIC + "/moo search [initial search term]"));
            this.searchResults.add(new LogEntry(EnumChatFormatting.GOLD + " 1) " + EnumChatFormatting.RESET + "Enter search term"));
            this.searchResults.add(new LogEntry(EnumChatFormatting.GOLD + " 2) " + EnumChatFormatting.RESET + "Adjust start and end date"));
            this.searchResults.add(new LogEntry(EnumChatFormatting.GOLD + " 3) " + EnumChatFormatting.RESET + "Select desired options (match case, ...)"));
            this.searchResults.add(new LogEntry(EnumChatFormatting.GOLD + " 4) " + EnumChatFormatting.RESET + "Click 'Search'"));
            this.searchResults.add(new LogEntry("" + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + "Search results"));
            this.searchResults.add(new LogEntry(EnumChatFormatting.GOLD + " - " + EnumChatFormatting.YELLOW + "CTRL + C " + EnumChatFormatting.RESET + "to copy selected search result"));
            this.searchResults.add(new LogEntry(EnumChatFormatting.GOLD + " - " + EnumChatFormatting.YELLOW + "CTRL + Shift + C " + EnumChatFormatting.RESET + "to copy selected search result " + EnumChatFormatting.ITALIC + "with" + EnumChatFormatting.RESET + " formatting codes"));
            this.searchResults.add(new LogEntry(EnumChatFormatting.GOLD + " - " + EnumChatFormatting.YELLOW + "CTRL + A " + EnumChatFormatting.RESET + "to copy all search results"));
            this.searchResults.add(new LogEntry(EnumChatFormatting.GOLD + " - " + EnumChatFormatting.YELLOW + "Double click search result " + EnumChatFormatting.RESET + "to open corresponding log file in default text editor"));
            this.guiSearchResults.setResults(searchResults);
        } else if (button == buttonSettings) {
            List<IConfigElement> logSearchConfigElements = new ArrayList<>();
            for (Property configEntry : Cowlection.getInstance().getConfig().getLogSearchProperties()) {
                logSearchConfigElements.add(new ConfigElement(configEntry));
            }
            mc.displayGuiScreen(new GuiConfig(this,
                    logSearchConfigElements,
                    Cowlection.MODID, "cowlectionLogSearchConfig", false, false,
                    EnumChatFormatting.GOLD + "Press Done to save changes."));
        }
    }

    private void setIsSearchInProgress(boolean isSearchInProgress) {
        this.isSearchInProgress = isSearchInProgress;
        buttonSearch.enabled = !isSearchInProgress;
        fieldSearchQuery.setEnabled(!isSearchInProgress);
        fieldDateStart.setEnabled(!isSearchInProgress);
        fieldDateEnd.setEnabled(!isSearchInProgress);
        checkboxChatOnly.enabled = !isSearchInProgress;
        checkboxMatchCase.enabled = !isSearchInProgress;
        checkboxRemoveFormatting.enabled = !isSearchInProgress;
        if (isSearchInProgress) {
            fieldSearchQuery.setFocused(false);
            fieldDateStart.setFocused(false);
            fieldDateEnd.setFocused(false);
            buttonSearch.displayString = EnumChatFormatting.ITALIC + "Searching";
            searchResults.clear();
            guiSearchResults.clearResults();
            analyzedFiles = null;
            analyzedFilesWithHits = null;
            skippedFiles = null;
        } else {
            buttonSearch.displayString = "Search";
        }
    }

    /**
     * List gui element similar to GuiModList.Info
     */
    class SearchResults extends GuiScrollingList {
        private final String[] spinner = new String[]{"oooooo", "Oooooo", "oOoooo", "ooOooo", "oooOoo", "ooooOo", "oooooO"};
        private final DateTimeFormatter coloredDateFormatter = DateTimeFormatter.ofPattern(EnumChatFormatting.GRAY + "HH" + EnumChatFormatting.DARK_GRAY + ":" + EnumChatFormatting.GRAY + "mm" + EnumChatFormatting.DARK_GRAY + ":" + EnumChatFormatting.GRAY + "ss");
        private List<LogEntry> rawResults;
        private List<IChatComponent> slotsData;
        /**
         * key: slot id of 1st line of a search result (if multi-line-result), value: search result id
         */
        private NavigableMap<Integer, Integer> searchResultEntries;
        private Pair<Long, String> errorMessage;
        private String resultsCount;

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
            int hoveredSlotId = this.func_27256_c(mouseX, mouseY);
            if (hoveredSlotId >= 0 && mouseY > top && mouseY < bottom) {
                float scrollDistance = getScrollDistance();
                if (scrollDistance != Float.MIN_VALUE) {
                    // draw hovered entry details

                    int hoveredSearchResultId = getSearchResultIdBySlotId(hoveredSlotId);
                    LogEntry hoveredEntry = getSearchResultByResultId(hoveredSearchResultId);
                    if (hoveredEntry != null && !hoveredEntry.isError()) {
                        // draw 'tooltips' in the top left corner
                        drawString(fontRendererObj, "Log file: ", 2, 2, 0xff888888);
                        GlStateManager.pushMatrix();
                        float scaleFactor = 0.75f;
                        GlStateManager.scale(scaleFactor, scaleFactor, 0);
                        fontRendererObj.drawSplitString(EnumChatFormatting.GRAY + Utils.toRealPath(hoveredEntry.getFilePath()), 5, (int) ((4 + fontRendererObj.FONT_HEIGHT) * (1 / scaleFactor)), (int) ((GuiSearch.this.fieldSearchQuery.xPosition - 8) * (1 / scaleFactor)), 0xff888888);
                        GlStateManager.popMatrix();
                        drawString(fontRendererObj, "Result: " + EnumChatFormatting.WHITE + (hoveredSearchResultId + 1) + EnumChatFormatting.RESET + "/" + EnumChatFormatting.WHITE + this.rawResults.size(), 8, 48, 0xff888888);
                        drawString(fontRendererObj, "Time: " + hoveredEntry.getTime().format(coloredDateFormatter), 8, 58, 0xff888888);
                    }

                    // formula from GuiScrollingList#drawScreen slotTop
                    int baseY = this.top + /* border: */4 - (int) scrollDistance;

                    // highlight multiline search results
                    Integer resultIndexStart = searchResultEntries.floorKey(hoveredSlotId);
                    Integer resultIndexEnd = searchResultEntries.higherKey(hoveredSlotId);

                    if (resultIndexStart == null) {
                        return;
                    } else if (resultIndexEnd == null) {
                        // last result entry
                        resultIndexEnd = getSize();
                    }

                    int slotTop = baseY + resultIndexStart * this.slotHeight - 2;
                    int slotBottom = baseY + resultIndexEnd * this.slotHeight - 2;
                    drawRect(this.left, Math.max(slotTop, top), right - /* scrollBar: */7, Math.min(slotBottom, bottom), 0x22ffffff);
                }
            } else if (areEntriesSearchResults) {
                if (analyzedFiles != null) {
                    drawString(fontRendererObj, analyzedFiles, 8, 15, 0xff888888);
                }
                if (analyzedFilesWithHits != null) {
                    drawString(fontRendererObj, analyzedFilesWithHits, 8, 25, 0xff888888);
                }
                if (skippedFiles != null) {
                    drawString(fontRendererObj, skippedFiles, 8, 35, 0xff888888);
                }
                if (resultsCount != null) {
                    drawString(fontRendererObj, resultsCount, 8, 50, 0xff888888);
                }
            }
            if (errorMessage != null) {
                if (errorMessage.first().compareTo(System.currentTimeMillis()) > 0) {
                    String errorText = "Error: " + EnumChatFormatting.RED + errorMessage.second();
                    int stringWidth = fontRendererObj.getStringWidth(errorText);
                    int margin = 5;
                    int left = width / 2 - stringWidth / 2 - margin;
                    int top = height / 2 - margin;
                    drawRect(left, top, left + stringWidth + 2 * margin, top + fontRendererObj.FONT_HEIGHT + 2 * margin, 0xff000000);
                    drawCenteredString(fontRendererObj, errorText, width / 2, height / 2, 0xffDD1111);
                } else {
                    errorMessage = null;
                }
            }
        }

        private float getScrollDistance() {
            try {
                return ReflectionHelper.getPrivateValue(GuiScrollingList.class, this, "scrollDistance");
            } catch (ReflectionHelper.UnableToAccessFieldException e) {
                e.printStackTrace();
                return Float.MIN_VALUE;
            }
        }

        @Override
        protected int getSize() {
            return slotsData.size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick) {
            if (doubleClick) {
                int searchResultIdBySlotId = getSearchResultIdBySlotId(index);
                LogEntry searchResult = rawResults.get(searchResultIdBySlotId);
                if (searchResult.getFilePath() == null) {
                    setErrorMessage("This log entry is not from a file");
                    return;
                }
                byte[] buffer = new byte[1024];
                String logFileName = Utils.toRealPath(searchResult.getFilePath());
                if (logFileName.endsWith("latest.log")) {
                    try {
                        Files.copy(searchResult.getFilePath(), mcLogOutputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else { // .log.gz
                    String newLine = System.getProperty("line.separator");
                    String fileHeader = "# Original filename: " + logFileName + newLine + "# Use CTRL + F to search for specific words" + newLine + newLine;
                    try (GZIPInputStream logFileGzipped = new GZIPInputStream(new FileInputStream(logFileName));
                         FileOutputStream logFileUnGzipped = new FileOutputStream(mcLogOutputFile)) {
                        logFileUnGzipped.write(fileHeader.getBytes());
                        int len;
                        while ((len = logFileGzipped.read(buffer)) > 0) {
                            logFileUnGzipped.write(buffer, 0, len);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    Desktop.getDesktop().open(mcLogOutputFile);
                } catch (IOException e) {
                    setErrorMessage("File extension .txt has no associated default editor");
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    setErrorMessage(e.getMessage()); // The file: <path> doesn't exist.
                    e.printStackTrace();
                } catch (UnsupportedOperationException e) {
                    setErrorMessage("Can't open files on this OS");
                    e.printStackTrace();
                }
            }
        }

        private void setErrorMessage(String errorMessage) {
            int showDuration = 10000; // ms
            this.errorMessage = Pair.of(System.currentTimeMillis() + showDuration, errorMessage);
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
            int drawnResultIndex = searchResultEntries.floorKey(slotIdx);
            if (Objects.equals(searchResultEntries.floorKey(selectedIndex), drawnResultIndex)) {
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

        private void setResults(List<LogEntry> searchResult) {
            this.rawResults = searchResult;
            this.slotsData = resizeContent(searchResult);
            if (GuiSearch.this.areEntriesSearchResults) {
                this.resultsCount = "Results: " + EnumChatFormatting.WHITE + this.rawResults.size();
            }
        }

        private void clearResults() {
            this.rawResults = Collections.emptyList();
            this.resultsCount = null;
            this.slotsData = resizeContent(Collections.emptyList());
        }

        private List<IChatComponent> resizeContent(List<LogEntry> searchResults) {
            this.searchResultEntries = new TreeMap<>();
            List<IChatComponent> slotsData = new ArrayList<>();
            for (int searchResultIndex = 0; searchResultIndex < searchResults.size(); searchResultIndex++) {
                LogEntry searchResult = searchResults.get(searchResultIndex);

                String searchResultEntry;
                if (searchResult.isError()) {
                    searchResultEntry = searchResult.getMessage();
                } else {
                    searchResultEntry = EnumChatFormatting.DARK_GRAY + searchResult.getTime().format(DateTimeFormatter.ISO_LOCAL_DATE) + " " + EnumChatFormatting.RESET + searchResult.getMessage();
                }
                searchResultEntries.put(slotsData.size(), searchResultIndex);
                List<IChatComponent> multilineResult = GuiUtilRenderComponents.splitText(new ChatComponentText(searchResultEntry), this.listWidth - 8, GuiSearch.this.fontRendererObj, false, true);
                slotsData.addAll(multilineResult);
            }
            return slotsData;
        }

        LogEntry getSelectedSearchResult() {
            int searchResultId = getSearchResultIdBySlotId(selectedIndex);
            return getSearchResultByResultId(searchResultId);
        }

        private LogEntry getSearchResultByResultId(int searchResultId) {
            return (searchResultId >= 0 && searchResultId < rawResults.size()) ? rawResults.get(searchResultId) : null;
        }

        private int getSearchResultIdBySlotId(int slotId) {
            Map.Entry<Integer, Integer> searchResultIds = searchResultEntries.floorEntry(slotId);
            return searchResultIds != null ? searchResultIds.getValue() : -1;
        }

        String getAllSearchResults() {
            return rawResults.stream().map(logEntry -> EnumChatFormatting.getTextWithoutFormattingCodes(logEntry.getMessage()))
                    .collect(Collectors.joining("\n"));
        }
    }
}
