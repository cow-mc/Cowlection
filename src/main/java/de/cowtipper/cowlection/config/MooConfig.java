package de.cowtipper.cowlection.config;

import com.google.common.collect.Maps;
import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.command.MooCommand;
import de.cowtipper.cowlection.command.TabCompletableCommand;
import de.cowtipper.cowlection.config.gui.MooConfigGui;
import de.cowtipper.cowlection.config.gui.MooConfigPreview;
import de.cowtipper.cowlection.data.DataHelper;
import de.cowtipper.cowlection.util.MooChatComponent;
import de.cowtipper.cowlection.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.command.ICommand;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.Util;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Mod configuration
 * <p>
 * Based on <a href="https://github.com/TheGreyGhost/MinecraftByExample/blob/1-8-9final/src/main/java/minecraftbyexample/mbe70_configuration/MBEConfiguration.java">TheGreyGhost's MinecraftByExample</a>
 *
 * @see ForgeModContainer
 */
public class MooConfig {
    // Category: General
    private static String configGuiExplanations;
    public static boolean hasOpenedConfigGui;
    public static String mooCmdAlias;
    public static boolean fixReplyCmd;
    public static boolean enableCopyInventory;
    private static String wailaLevelOfDetail;
    private static String copyOrSaveWailaAndInventoryData;
    public static String[] tabCompletableNamesCommands;
    private static final String CATEGORY_LOGS_SEARCH = "logssearch";
    public static String[] logsDirs;
    private static String defaultStartDate;
    private static int maxLogFileSize;
    // Category: Notifications
    public static boolean doUpdateCheck;
    public static boolean showBestFriendNotifications;
    public static boolean enableBestFriendNotificationSound;
    public static boolean showFriendNotifications;
    public static boolean showGuildNotifications;
    public static boolean doBestFriendsOnlineCheck;
    // Category: SkyBlock
    private static String enableSkyBlockOnlyFeatures;
    public static int notifyFreshServer;
    public static int notifyOldServer;
    public static boolean notifyServerAge;
    public static boolean chestAnalyzerShowNonBazaarItems;
    private static String chestAnalyzerUseBazaarPrices;
    public static boolean chestAnalyzerShowCommandUsage;
    public static int tooltipToggleKeyBinding;
    private static String tooltipItemAge;
    public static boolean tooltipItemAgeShortened;
    private static String tooltipItemTimestamp;
    private static String showPetExp;
    private static String numeralSystem;
    private static String tooltipAuctionHousePriceEach;
    public static String[] tooltipAuctionHousePriceEachEnchantments;
    private static String auctionHouseMarkEndedAuctions;
    public static String bazaarSellAllOrder;
    public static String bazaarSellAllOrderAscDesc;
    private static String bazaarConnectGraphsNodes;
    public static int bazaarConnectGraphsLineWidth;
    public static String bestiaryOverviewOrder;
    private String[] bestiaryOverviewOrderDefaultValues;
    private static int lookupWikiKeyBinding;
    private static int lookupPriceKeyBinding;
    public static boolean lookupItemDirectly;
    // Category: SkyBlock Dungeons
    private static String showItemQualityAndFloor;
    private static String dungItemQualityPos;
    public static boolean dungItemQualityShortenNonRandomized;
    public static boolean dungItemHideGearScore;
    public static int dungItemToolTipToggleKeyBinding;
    public static boolean dungSendPerformanceOnDeath;
    public static boolean dungSendPerformanceOnEndScreen;
    public static boolean dungOverlayEnabled;
    public static int dungOverlayPositionX;
    public static int dungOverlayPositionY;
    public static int dungOverlayGuiScale;
    private static String dungOverlayTextBorder;
    private static String dungPartyFinderPlayerLookup;
    public static boolean dungPartyFullLookup;
    public static boolean dungPartyFinderPartyLookup;
    public static boolean dungPartyFinderOverlayDrawBackground;
    public static boolean dungPartiesSize;
    public static int dungDungeonReqMin;
    public static int dungClassMin;
    private static String dungMarkPartiesWithCarry;
    private static String dungMarkPartiesWithHyperion;
    private static String dungMarkPartiesWithArcher;
    private static String dungMarkPartiesWithBerserk;
    private static String dungMarkPartiesWithHealer;
    private static String dungMarkPartiesWithMage;
    private static String dungMarkPartiesWithTank;
    public static boolean dungSendWrongFloorWarning;

    private static Configuration cfg = null;
    private static final List<MooConfigCategory> configCategories = new ArrayList<>();
    private final Cowlection main;
    private Property propMooCmdAlias;
    private Property propTabCompletableNamesCommands;
    private Property propTooltipAuctionHousePriceEachEnchantments;
    private List<Property> logSearchProperties;

    public MooConfig(Cowlection main, Configuration configuration) {
        this.main = main;
        cfg = configuration;

        String oldLoadedConfigVersion = cfg.getLoadedConfigVersion();
        boolean configVersionChanged = oldLoadedConfigVersion == null || !oldLoadedConfigVersion.equals(cfg.getDefinedConfigVersion());
        if (configVersionChanged) {
            updateConfigPreInit(oldLoadedConfigVersion);
        }
        initConfig();
        if (configVersionChanged) {
            updateConfigPostInit(oldLoadedConfigVersion);
        }
    }

    private void updateConfigPreInit(String oldVersion) {
        if (oldVersion == null) {
            // config of Cowlection v1.8.9-0.10.2 and older

            // leave log search settings as is

            if (cfg.hasCategory(Configuration.CATEGORY_CLIENT)) {
                // copy old 'moo' value to new, separate config
                if (cfg.hasKey(Configuration.CATEGORY_CLIENT, "moo")) {
                    String oldMoo = cfg.getString("moo", Configuration.CATEGORY_CLIENT, "00000000-0000-0000-0000-000000000000", "Temporary config entry, should be deleted automatically.", Utils.VALID_UUID_PATTERN);
                    if (StringUtils.isNotEmpty(oldMoo) && Utils.isValidUuid(oldMoo)) {
                        // save into new cfg:
                        main.getMoo().setMooIfValid(oldMoo, false);
                    }
                }

                // delete client category (no longer used)
                ConfigCategory oldClientCategory = cfg.getCategory(Configuration.CATEGORY_CLIENT);
                cfg.removeCategory(oldClientCategory);
            }
            cfg.save();
        }
    }

    private void updateConfigPostInit(String oldVersion) {
        if ("1".equals(oldVersion)) {
            // config of Cowlection v1.8.9-0.12.0 and older
            ConfigCategory sbDungCategory = cfg.getCategory("skyblockdungeons");
            if (sbDungCategory.containsKey("dungOverlayTextShadow")) {
                boolean dungOverlayTextShadow = sbDungCategory.get("dungOverlayTextShadow").getBoolean();
                if (!dungOverlayTextShadow) {
                    sbDungCategory.get("dungOverlayTextBorder").set("no border");
                }
                sbDungCategory.remove("dungOverlayTextShadow");
            }

            for (String dungClass : new String[]{"Archer", "Berserk", "Healer", "Mage", "Tank"}) {
                String configKey = "dungFilterPartiesWith" + dungClass + "Dupes";
                if (sbDungCategory.containsKey(configKey)) {
                    boolean filterPartiesWithX = sbDungCategory.get(configKey).getBoolean();
                    String configKeyNew = "dungMarkPartiesWith" + dungClass;
                    if (filterPartiesWithX) {
                        sbDungCategory.get(configKeyNew).set("if duplicated");
                    }
                    sbDungCategory.remove(configKey);
                }
            }
            cfg.save();
            syncFromFile();
        }
    }

    private void initConfig() {
        syncFromFile();
        MinecraftForge.EVENT_BUS.register(new ConfigEventHandler());
    }

    /**
     * Load the configuration values from the configuration file
     */
    private void syncFromFile() {
        syncConfig(true, true, true);
    }

    /**
     * Save the GUI-altered values to disk
     */
    public void syncFromGui() {
        syncConfig(false, true, true);
    }

    /**
     * Save the GUI-altered values to the properties; don't save to disk - only memory
     */
    public void syncFromGuiWithoutSaving() {
        syncConfig(false, true, false);
    }

    /**
     * Save the Configuration variables (fields) to disk
     */
    public void syncFromFields() {
        syncConfig(false, false, true);
    }

    /**
     * Synchronise the three copies of the data
     * 1) loadConfigFromFile && readFieldsFromConfig -> initialise everything from the disk file
     * 2) !loadConfigFromFile && readFieldsFromConfig -> copy everything from the config file (altered by GUI)
     * 3) !loadConfigFromFile && !readFieldsFromConfig -> copy everything from the native fields
     *
     * @param loadConfigFromFile   if true, load the config field from the configuration file on disk
     * @param readFieldsFromConfig if true, reload the member variables from the config field
     * @param saveToFile           if true, save changes to config file
     */
    @SuppressWarnings("DuplicatedCode")
    private void syncConfig(boolean loadConfigFromFile, boolean readFieldsFromConfig, boolean saveToFile) {
        if (loadConfigFromFile) {
            cfg.load();
        }

        // reset previous entries
        configCategories.clear();

        // Category: General
        MooConfigCategory configCat = new MooConfigCategory("General", "general");
        configCategories.add(configCat);

        // Sub-Category: Cowlection config gui
        MooConfigCategory.SubCategory subCat = configCat.addSubCategory("Cowlection config gui");
        subCat.addExplanations("Display of the explanations for each sub-section marked with §2❢§r",
                "  ‣ " + EnumChatFormatting.YELLOW + "as tooltip" + EnumChatFormatting.RESET + " = tooltip when hovering over sub-category heading",
                "  ‣ " + EnumChatFormatting.YELLOW + "as text" + EnumChatFormatting.RESET + " = below each sub-category heading",
                "  ‣ " + EnumChatFormatting.YELLOW + "hidden" + EnumChatFormatting.RESET + " = ");
        Property propConfigGuiExplanations = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "configGuiExplanations", "as tooltip", "Display config settings explanations",
                new String[]{"as tooltip", "as text", "hidden"}));


        // (not visible in config gui: has opened config? or: knows how to move the dungeon overlay?)
        Property propHasOpenedConfigGui = cfg.get(configCat.getConfigName(),
                "hasOpenedConfigGui", false, "Already opened config gui?")
                .setShowInGui(false);

        // Sub-Category: API settings
        subCat = configCat.addSubCategory("API settings");
        subCat.addExplanations("Some features use the official Hypixel API and therefore require your API key.",
                "Use " + EnumChatFormatting.YELLOW + "/moo apikey " + EnumChatFormatting.RESET + "to see how to request a new API key from Hypixel",
                "The API key is stored " + EnumChatFormatting.ITALIC + "locally " + EnumChatFormatting.ITALIC + "on your computer.");
        subCat.addConfigEntry(main.getMoo().getPropIsMooValid());

        // Sub-Category: Command settings
        subCat = configCat.addSubCategory("Command settings");

        propMooCmdAlias = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "mooCmdAlias", "m", "Alias for /moo command")
                .setValidationPattern(Pattern.compile("^[A-Za-z]*$")));
        Property propFixReplyCmd = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "fixReplyCmd", true, "Auto-replace /r?"));
        Property propEnableCopyInventory = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "enableCopyInventory", false, "Enable copy inventory with CTRL + C?"));
        Property propWailaLevelOfDetail = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "wailaLevelOfDetail", "main info", "Level of detail of /moo waila", new String[]{"main info", "all info"}));
        Property propCopyOrSaveWailaAndInventoryData = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "copyOrSaveWailaAndInventoryData", "to clipboard", "Copy or save waila and inventory data?", new String[]{"to clipboard", "save file"}));

        // Sub-Category: Tab-completable names in commands
        subCat = configCat.addSubCategory("Tab-completable usernames");
        subCat.addExplanations("For certain commands you can use " + EnumChatFormatting.YELLOW + "TAB " + EnumChatFormatting.RESET + "to autocomplete player names",
                EnumChatFormatting.UNDERLINE + "Uses player names from:",
                "  ‣ Guild and Party chat",
                "  ‣ Party and game (duels) invites",
                "  ‣ SkyBlock Dungeon party finder: when a player joins the group",
                "  ‣ Online best friends (if the best friend online checker is enabled)");

        propTabCompletableNamesCommands = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "tabCompletableNamesCommands", new String[]{"p", "ah", "ignore", "msg", "tell", "w", "boop", "profile", "friend", "friends"}, "List of commands with a Tab-completable username argument.")
                .setValidationPattern(Pattern.compile("^[A-Za-z]+$")));

        // Sub-Category: Other settings
        subCat = configCat.addSubCategory("Other settings");
        subCat.addExplanations("Other settings that are located in other GUIs");

        Property propLogsDirs = subCat.addConfigEntry(cfg.get(CATEGORY_LOGS_SEARCH,
                "logsDirs", resolveDefaultLogsDirs(),
                "Directories with Minecraft log files"));
        Property propDefaultStartDate = subCat.addConfigEntry(cfg.get(CATEGORY_LOGS_SEARCH,
                "defaultStartDate", "3", "Default start date (a number means X months ago, alternatively a fixed date à la yyyy-mm-dd can be used)"))
                .setValidationPattern(Pattern.compile("^[1-9][0-9]{0,2}|(2[0-9]{3}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01]))$"));
        Property propMaxLogFileSize = subCat.addConfigEntry(cfg.get(CATEGORY_LOGS_SEARCH,
                "maxLogFileSize", 2048, "Max log file size (in KB)?", 50, 10000));
        logSearchProperties = new ArrayList<>();
        logSearchProperties.add(propLogsDirs);
        logSearchProperties.add(propDefaultStartDate);
        logSearchProperties.add(propMaxLogFileSize);

        // Category: Notifications
        configCat = new MooConfigCategory("Notifications", "notifications");
        configCategories.add(configCat);

        // Sub-Category: Mod update checker
        subCat = configCat.addSubCategory("Mod update checker");
        Property propDoUpdateCheck = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "doUpdateCheck", true, "Check for mod updates?"));

        // Sub-Category: Login & Logout
        subCat = configCat.addSubCategory("Login & Logout");
        subCat.addExplanations("Hides selected login/logout notifications ",
                "while still showing notifications of best friends (if enabled).",
                "Add someone to the best friends list with " + EnumChatFormatting.YELLOW + "/moo add <player>" + EnumChatFormatting.RESET);

        Property propShowBestFriendNotifications = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "showBestFriendNotifications", true, "Set to true to receive best friends' login/logout messages, set to false hide them."),
                new MooConfigPreview(new ChatComponentText("§a§lBest friend §a> §6Cow §r§ejoined.")));
        Property propEnableBestFriendNotificationSound = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "enableBestFriendNotificationSound", false, "Set to true to play a notification sound when a best friend comes online"),
                new MooConfigPreview("random.pop", Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MASTER), 1));

        Property propShowFriendNotifications = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "showFriendNotifications", true, "Set to true to receive friends' login/logout messages, set to false hide them."),
                new MooConfigPreview(new ChatComponentText("§aFriend > §r§aBob §ejoined.")));
        Property propShowGuildNotifications = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "showGuildNotifications", true, "Set to true to receive guild members' login/logout messages, set to false hide them."),
                new MooConfigPreview(new ChatComponentText("§2Guild > §r§7Herobrian §eleft.")));


        // Sub-Category: Best friends online status
        subCat = configCat.addSubCategory("Best friend online checker");
        subCat.addExplanations("Check which best friends are online when you join the server.",
                "About once a day, a check for new name changes is also performed automatically.");

        IChatComponent spacer = new MooChatComponent(", ").green();
        Property propDoBestFriendsOnlineCheck = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "doBestFriendsOnlineCheck", true, "Set to true to check best friends' online status when joining a server, set to false to disable."),
                new MooConfigPreview(new MooChatComponent("§a⬤ Online best friends (§24§a/§216§a): ")
                        .appendSibling(MooConfigPreview.createDemoOnline("Alice", "Housing", "1 hour 13 minutes 37 seconds")).appendSibling(spacer)
                        .appendSibling(MooConfigPreview.createDemoOnline("Bob", "Build Battle", "2 hours 13 minutes 37 seconds")).appendSibling(spacer)
                        .appendSibling(MooConfigPreview.createDemoOnline("Cow", "SkyBlock", "13 minutes 37 seconds")).appendSibling(spacer)
                        .appendSibling(MooConfigPreview.createDemoOnline("Herobrian", "Murder Mystery", "13 hours 33 minutes 37 seconds"))));


        // Category: SkyBlock
        configCat = new MooConfigCategory("SkyBlock", "skyblock");
        configCategories.add(configCat);

        // Sub-Category: SkyBlock-only features
        subCat = configCat.addSubCategory("SkyBlock-only features");
        Property propEnableSkyBlockOnlyFeatures = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "enableSkyBlockOnlyFeatures", "on SkyBlock", "Enable SkyBlock-only features?", new String[]{"on SkyBlock", "always", "never"}));

        // Sub-Category: Server age notifications
        subCat = configCat.addSubCategory("Server age notifications");
        subCat.addExplanations("Servers usually restart once they exceed " + EnumChatFormatting.YELLOW + "30-38 ingame days " + EnumChatFormatting.RESET + "(10-13 hours)",
                "Use the command " + EnumChatFormatting.YELLOW + "/moo worldage " + EnumChatFormatting.RESET + "to check how long the current world is loaded.",
                EnumChatFormatting.ITALIC + "Set a value to 0 to disable that notification.");
        Property propNotifyFreshServer = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "notifyFreshServer", 1, "Notify when a world is loaded <X ingame days", 0, 40));
        Property propNotifyOldServer = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "notifyOldServer", 30, "Notify when a world is loaded >X ingame days", 0, 40));
        Property propNotifyServerAge = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "notifyServerAge", true, "Show server age notifications?"));

        // Sub-Category: Chest Analyzer (Bazaar prices)
        subCat = configCat.addSubCategory("Chest Tracker & Analyzer (Bazaar prices)");
        String analyzeCommand = "/moo analyzeChests";
        subCat.addExplanations("Use " + EnumChatFormatting.YELLOW + analyzeCommand + EnumChatFormatting.RESET + " to start tracking chests on your island! " + EnumChatFormatting.GREEN + "Then you can...",
                EnumChatFormatting.GREEN + "  ❶ " + EnumChatFormatting.RESET + "add chests by opening them; deselect chests by Sneaking + Right Click.",
                EnumChatFormatting.GREEN + "  ❷ " + EnumChatFormatting.RESET + "use " + EnumChatFormatting.YELLOW + analyzeCommand + EnumChatFormatting.RESET + " again to run the chest analysis.",
                EnumChatFormatting.GREEN + "  ❸ " + EnumChatFormatting.RESET + "use " + EnumChatFormatting.YELLOW + analyzeCommand + " stop" + EnumChatFormatting.RESET + " to stop the chest tracker and clear current results.");
        Property propChestAnalyzerShowNonBazaarItems = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "chestAnalyzerShowNonBazaarItems", false, "Show non-Bazaar items in Chest Tracker?"));
        Property propChestAnalyzerUseBazaarPrices = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "chestAnalyzerUseBazaarPrices", "Instant-Sell", "Use Bazaar prices?", new String[]{"Instant-Sell", "Sell Offer"}));
        Property propChestAnalyzerShowCommandUsage = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "chestAnalyzerShowCommandUsage", true, "Show command usage?"));

        // Sub-Category: Tooltip enhancements
        subCat = configCat.addSubCategory("Tooltip & GUI enhancements");

        Property propTooltipToggleKeyBinding = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "tooltipToggleKeyBinding", Keyboard.KEY_LSHIFT, "Key to toggle tooltip"));

        Map<String, NBTBase> demoItemExtraAttributes = new HashMap<>();
        demoItemExtraAttributes.put("new_years_cake", new NBTTagInt(1));
        demoItemExtraAttributes.put("originTag", new NBTTagString("REWARD_NEW_YEARS_CAKE_NPC"));
        demoItemExtraAttributes.put("id", new NBTTagString("NEW_YEAR_CAKE"));
        demoItemExtraAttributes.put("uuid", new NBTTagString("64b3a60b-74f2-4ebd-818d-d019c5b7f3e0"));
        demoItemExtraAttributes.put("timestamp", new NBTTagString("6/16/19 5:05 PM"));
        MooConfigPreview nonStackableItemPreview = new MooConfigPreview(MooConfigPreview.createDemoItem("cake", "§dNew Year Cake", new String[]{"§7Given to every player as a", "§7celebration for the 1st SkyBlock", "§7year!", "", "§d§lSPECIAL"}, demoItemExtraAttributes));

        Property propTooltipItemAge = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "tooltipItemAge", "always", "Show item age", new String[]{"always", "key press", "never"}),
                nonStackableItemPreview);

        Property propTooltipItemAgeShortened = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "tooltipItemAgeShortened", true, "Shorten item age?"));

        Property propTooltipItemTimestamp = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "tooltipItemTimestamp", "key press", "Show item creation date", new String[]{"always", "key press", "never"}));

        Property propShowPetExp = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "showPetExp", "always", "Show pet exp", new String[]{"always", "key press", "never"}));

        Property propNumeralSystem = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "numeralSystem", "Arabic: 1, 4, 10", "Use Roman or Arabic numeral system?", new String[]{"Arabic: 1, 4, 10", "Roman: I, IV, X"}));

        Map<String, NBTBase> demoAhItemExtraAttributes = new HashMap<>();
        demoAhItemExtraAttributes.put("id", new NBTTagString("BEACON"));
        ItemStack demoAhItem = MooConfigPreview.createDemoItem("beacon", "§764x §fB§8e§facon Block", new String[]{"§f§lCOMMON", "§8§m-----------------", "§7Seller: §6[MVP§0++§6] Enlightener", "§7Buy it now: §63,900,000 coins", "", "§7Ends in: §e13h 33m 37s", "", "§eDon't click to inspect!"}, demoAhItemExtraAttributes);
        demoAhItem.stackSize = 64;
        MooConfigPreview ahItemPreview = new MooConfigPreview(demoAhItem);
        Property propTooltipAuctionHousePriceEach = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "tooltipAuctionHousePriceEach", "always", "Add price per item if multiple items are bought or sold", new String[]{"always", "key press", "never"}), ahItemPreview);

        propTooltipAuctionHousePriceEachEnchantments = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "tooltipAuctionHousePriceEachEnchantments", new String[]{"overload", "rejuvenate"}, "Price per lvl 1 book enchantment")
                .setValidationPattern(Pattern.compile("^[A-Za-z_ -]+$")));

        Property propAuctionHouseMarkEndedAuctions = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "auctionHouseMarkEndedAuctions", "a letter", "Mark ended auctions", new String[]{"a letter", "a word", "disabled"}));

        Property propBazaarSellAllOrder = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "bazaarSellAllOrder", "price (sum)", "Bazaar: sell all order", new String[]{"price (sum)", "item amount", "unordered", "price (each)"}),
                new MooConfigPreview(MooConfigPreview.createDemoItem("chest", "§aSell Inventory Now", new String[]{"§7Instantly sell anything in", "§7your inventory that can be", "§7sold on the Bazaar.", "", " §a1§7x §aEnchanted Leather §7for §65,263.1 coins", " §a42§7x §fLeather §7for §6436.8 coins", " §a2§7x §fRabbit Hide §7for §642.0 coins", " §a79§7x §fRaw Beef §7for §6450.3 coins", " §a16§7x §aEnchanted Raw Beef §7for §69,867.2 coins", "", "§7You earn: §615,698 coins", "", "§eClick to sell!"}, Collections.emptyMap())));

        Property propBazaarSellAllOrderAscDesc = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "bazaarSellAllOrderAscDesc", "low → high", "Bazaar: sell all order asc/desc", new String[]{"low → high", "high → low"}));

        MooConfigPreview bazaarGraphPreview = new MooConfigPreview(MooConfigPreview.createDemoItem("paper", "§aBuy Price §731d §77d §e24h", new String[]{
                "§7The price at which buy orders have been filled.", "",
                "§r┌----------------------------------------------┐", "§r│§66. 1k§r+§bxxxxxx§8·································§bxx§r│",
                "§r│§8····§r│§8······································§bx§8··§r│", "§r│§66. 1k§r+§8·····§bx§8···················§bx§8·······§bxxxxx§8···§r│",
                "§r│§8····§r│§8···············§bx§8········§bxxxxxxxxx§8········§r│", "§r│§8··§66k§r+§8··············§bx§8····§bxx§8··§bx§8·················§r│",
                "§r│§8····§r│§8············§bx§8··§bxxxx§8·§bxxx§8··················§r│", "§r│§8··§66k§r+§8······§bx§8·§bxxxx§8·§bx§8···························§r│",
                "§r│§8····§r│§8·······§bx§8·································§r│", "§r│§8··§66k§r+---------+----------+---------+---------+│",
                "§r│§8····§r24h§8······§r18h§8········§r12h§8·······§r6h§8·······§rnow│", "§r└----------------------------------------------┘"}, Maps.newHashMap()));
        Property propBazaarConnectGraphsNodes = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "bazaarConnectGraphsNodes", "always", "Bazaar: connect the graph nodes", new String[]{"always", "key press", "never"}),
                bazaarGraphPreview);

        Property propBazaarConnectGraphsLineWidth = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "bazaarConnectGraphsLineWidth", 3, "Line width of bazaar graph", 1, 10));

        bestiaryOverviewOrderDefaultValues = new String[]{"fewest kills", "lowest %", "hidden"};
        MooConfigPreview bestiaryOverviewPreview = new MooConfigPreview(MooConfigPreview.createDemoItem("wheat", "§a§3The Barn", new String[]{
                "§7View all of the mobs that you've",
                "§7found and killed in §3The Barn§7.",
                "",
                "§7Families Found: §e75§6%", "§3---------------§f----- §b3§3/§b4"}, Maps.newHashMap()));
        Property propBestiaryOverviewOrder = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "bestiaryOverviewOrder", "fewest kills", "Order of the Bestiary overview?", bestiaryOverviewOrderDefaultValues),
                bestiaryOverviewPreview);

        // Sub-Category: Item lookup
        subCat = configCat.addSubCategory("Item lookup");
        subCat.addExplanations("Lookup item prices or wiki articles for any SkyBlock item in any inventory.");

        Property propLookupWikiKeyBinding = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "lookupWikiKeyBinding", Keyboard.KEY_I, "Key to lookup wiki"));
        Property propLookupPriceKeyBinding = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "lookupPriceKeyBinding", Keyboard.KEY_P, "Key to lookup item price"));

        Property propLookupItemDirectly = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "lookupItemDirectly", true, "Open website directly?"));

        // Category: SkyBlock Dungeons
        configCat = new MooConfigCategory("SkyBlock Dungeons", "skyblockdungeons");
        configCat.setMenuDisplayName("SB Dungeons");
        configCategories.add(configCat);

        // Sub-Category: Tooltip enhancements
        subCat = configCat.addSubCategory("Dungeon item tooltip enhancements");
        subCat.addExplanations("Shows " + EnumChatFormatting.YELLOW + "item quality " + EnumChatFormatting.RESET + "and " + EnumChatFormatting.YELLOW + "dungeon floor" + EnumChatFormatting.RESET + " (if enabled)",
                "",
                "Hold left " + EnumChatFormatting.YELLOW + "SHIFT " + EnumChatFormatting.RESET + "while hovering over a dungeon item,",
                "to remove stats from reforges and essences (✪)",
                "which normally makes the comparison of dungeon items difficult.",
                "Instead, the tooltip shows...",
                "  ‣ base/default stats " + EnumChatFormatting.GRAY + "(outside dungeons; 1st value - usually red or green)",
                "  ‣ stats inside dungeons " + EnumChatFormatting.GRAY + "(including dungeon level stat boost, but without essences [₀ₓ✪])",
                "  ‣ stats inside dungeons with 5x essence upgrades " + EnumChatFormatting.GRAY + "(₅ₓ✪)");

        Property propShowItemQualityAndFloor = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "showItemQualityAndFloor", "always", "show item quality + obtained floor?", new String[]{"always", "key press", "never"}));

        Property propDungItemQualityPos = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungItemQualityPos", "top", "Position of item quality in tooltip", new String[]{"top", "bottom"}),
                new MooConfigPreview(
                        MooConfigPreview.createDungeonItem("light", "7/17/20 7:22 PM", "§7Gear Score: §d336 §8(526)", "§7Crit Chance: §c+5% §9(Light +2%)", "§7Crit Damage: §c+30% §9(Light +4%) §8(+48.9%)", "§7Bonus Attack Speed: §c+4% §9(Light +4%)", "", "§7Health: §a+126 HP §9(Light +15 HP) §8(+205.38 HP)", "§7Defense: §a+76 §9(Light +4) §8(+123.88)", "§7Speed: §a+4 §9(Light +4) §8(+6.52)", "", "§9Growth V, §9Protection V", "§9Thorns III", "", "§7Increase the damage you deal", "§7with arrows by §c5%§7.", "", "§6Full Set Bonus: Skeleton Soldier", "§7Increase the damage you deal", "§7with arrows by an extra §c25%§7.", "", "§aPerfect 52500 / 52500", "§5§lEPIC DUNGEON LEGGINGS"),
                        MooConfigPreview.createDungeonItem("clean", "7/11/20 12:27 PM", "§7Gear Score: §d359 §8(561)", "§7Crit Chance: §c+11% §9(Clean +8%)", "§7Crit Damage: §c+26% §8(+42.38%)", "", "§7Health: §a+126 HP §9(Clean +15 HP) §8(+205.38 HP)", "§7Defense: §a+87 §9(Clean +15) §8(+141.81)", "", "§9Growth V, §9Protection V", "§9Thorns III", "", "§7Increase the damage you deal", "§7with arrows by §c5%§7.", "", "§6Full Set Bonus: Skeleton Soldier", "§7Increase the damage you deal", "§7with arrows by an extra §c25%§7.", "", "§aPerfect 52500 / 52500", "§5§lEPIC DUNGEON LEGGINGS")));

        Property propDungItemQualityShortenNonRandomized = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungItemQualityShortenNonRandomized", false, "Shorten item quality for non-randomized items?"));

        Property propDungItemHideGearScore = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungItemHideGearScore", false, "Hide Gear Score?"));

        Property propDungItemToolTipToggleKeyBinding = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungItemToolTipToggleKeyBinding", Keyboard.KEY_LSHIFT, "Key to toggle dungeon item tooltip"));

        // Sub-Category: Dungeon Performance Overlay
        subCat = configCat.addSubCategory("Dungeon Performance Overlay");
        subCat.addExplanations(EnumChatFormatting.UNDERLINE + "Keeps track of:",
                "  ‣ skill score " + EnumChatFormatting.GRAY + "(reduced by deaths and failed puzzles)",
                "  ‣ speed score " + EnumChatFormatting.GRAY + "(-2.2 points/minute when over 20 minutes)",
                "  ‣ bonus score " + EnumChatFormatting.GRAY + "(+1 [max 5] for each destroyed crypt; if 'enhanced tab list' is disabled: limited to ~50 blocks away from the player)",
                "Does " + EnumChatFormatting.ITALIC + "not" + EnumChatFormatting.RESET + " track explorer score " + EnumChatFormatting.GRAY + "(explored rooms, secrets, ...)");

        Property propDungSendPerformanceOnDeath = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungSendPerformanceOnDeath", true, "Send dungeon performance after a player died?"));

        Property propDungSendPerformanceOnEndScreen = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungSendPerformanceOnEndScreen", true, "Send dungeon performance on end screen?"));

        Property propDungOverlayEnabled = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungOverlayEnabled", true, "Enable Dungeon performance overlay?"));

        Property propDungOverlayPositionX = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungOverlayPositionX", 1, "Dungeon performance overlay position: x value", 0, 1000),
                null, "‰", // per mille
                (slider) -> {
                    MooConfig.dungOverlayPositionX = slider.getValueInt();
                    MooConfigGui.showDungeonPerformanceOverlayUntil = System.currentTimeMillis() + 500;
                });

        Property propDungOverlayPositionY = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungOverlayPositionY", 1, "Dungeon performance overlay position: y value", 0, 1000),
                null, "‰", // per mille
                (slider) -> {
                    MooConfig.dungOverlayPositionY = slider.getValueInt();
                    MooConfigGui.showDungeonPerformanceOverlayUntil = System.currentTimeMillis() + 500;
                });

        Property propDungOverlayGuiScale = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungOverlayGuiScale", 100, "Dungeon performance overlay GUI scale", 50, 200),
                null, "%",
                (slider) -> {
                    MooConfig.dungOverlayGuiScale = slider.getValueInt();
                    MooConfigGui.showDungeonPerformanceOverlayUntil = System.currentTimeMillis() + 500;
                });

        Property propDungOverlayTextBorder = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungOverlayTextBorder", "drop shadow", "Dungeon performance overlay text border", new String[]{"drop shadow", "full outline", "no border"}));

        // Sub-Category: Party Finder
        subCat = configCat.addSubCategory("Dungeon Party Finder");
        subCat.addExplanations("Adds various indicators to the dungeon party finder",
                "to make it easier to find the perfect party:",
                "",
                "Marks parties...",
                "  ‣ you cannot join: " + EnumChatFormatting.RED + "⬛",
                "  ‣ that do not meet all your criteria: " + EnumChatFormatting.GOLD + "⬛",
                "    ‣ with \"Dungeon Level Required\" below a certain level " + EnumChatFormatting.GRAY + "(if present)" + EnumChatFormatting.RESET + ": " + EnumChatFormatting.DARK_RED + EnumChatFormatting.BOLD + "ᐯ" + EnumChatFormatting.RESET,
                "    ‣ with someone below a certain class level: " + EnumChatFormatting.RED + EnumChatFormatting.BOLD + "ᐯ" + EnumChatFormatting.RESET,
                "    ‣ with duplicated roles you specify: " + EnumChatFormatting.GOLD + "²⁺",
                "    ‣ with someone with a role you specify: " + EnumChatFormatting.GRAY + "e.g. " + EnumChatFormatting.WHITE + "H " + EnumChatFormatting.GRAY + "(class' 1ˢᵗ letter)",
                "  ‣ that match your criteria: " + EnumChatFormatting.GREEN + "⬛");

        Property propDungPartyFinderPlayerLookup = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungPartyFinderPlayerLookup", "as a tooltip", "Show armor + dungeons stats of player joining via party finder as a tooltip or in chat?", new String[]{"as a tooltip", "in chat", "disabled"}));

        Property propDungPartyFullLookup = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungPartyFullLookup", true, "Lookup info when party full?"));

        Property propDungPartyFinderPartyLookup = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungPartyFinderPartyLookup", true, "Lookup info when joining another party?"));

        Property propDungPartyFinderOverlayDrawBackground = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungPartyFinderOverlayDrawBackground", true, "Party Finder: draw colored overlay?"));

        Property propDungPartiesSize = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungPartiesSize", true, "Show size of parties?"),
                new MooConfigPreview(new MooChatComponent("Marked with: " + EnumChatFormatting.WHITE + "1 - 4").gray()));

        Property propDungDungeonReqMin = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungDungeonReqMin", 0, "Marks parties with lower Dungeon level req than this value")
                        .setMinValue(0).setMaxValue(50),
                new MooConfigPreview(new MooChatComponent("Marked with: " + EnumChatFormatting.DARK_RED + EnumChatFormatting.BOLD + "ᐯ").gray()));

        Property propDungClassMin = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungClassMin", 0, "Marks parties with members with lower class level than this value")
                        .setMinValue(0).setMaxValue(50),
                new MooConfigPreview(new MooChatComponent("Marked with: " + EnumChatFormatting.RED + EnumChatFormatting.BOLD + "ᐯ").gray()));

        Property propDungMarkPartiesWithCarry = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungMarkPartiesWithCarry", "do not mark", "Mark parties with carry in the notes",
                new String[]{"suitable " + EnumChatFormatting.GREEN + "⬛", "unideal " + EnumChatFormatting.GOLD + "⬛", "block " + EnumChatFormatting.RED + "⬛", "do not mark"}),
                new MooConfigPreview(new MooChatComponent("Marked with: " + EnumChatFormatting.AQUA + "carry"
                        + EnumChatFormatting.GRAY + " or " + EnumChatFormatting.GREEN + "carry " + EnumChatFormatting.GRAY + "('free' carries)").gray()));

        Property propDungMarkPartiesWithHyperion = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungMarkPartiesWithHyperion", "do not mark", "Mark parties with hyperion in the notes",
                new String[]{"suitable " + EnumChatFormatting.GREEN + "⬛", "unideal " + EnumChatFormatting.GOLD + "⬛", "block " + EnumChatFormatting.RED + "⬛", "do not mark"}),
                new MooConfigPreview(new MooChatComponent("Marked with: " + EnumChatFormatting.AQUA + "hyper").gray()));

        Property propDungMarkPartiesWithArcher = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungMarkPartiesWithArcher", "if duplicated", "Mark parties with Archer class?", new String[]{"always", "if duplicated", "do not mark"}),
                new MooConfigPreview(DataHelper.DungeonClass.ARCHER));

        Property propDungMarkPartiesWithBerserk = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungMarkPartiesWithBerserk", "do not mark", "Mark parties with Berserk class?", new String[]{"always", "if duplicated", "do not mark"}),
                new MooConfigPreview(DataHelper.DungeonClass.BERSERK));

        Property propDungMarkPartiesWithHealer = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungMarkPartiesWithHealer", "do not mark", "Mark parties with Healer class?", new String[]{"always", "if duplicated", "do not mark"}),
                new MooConfigPreview(DataHelper.DungeonClass.HEALER));

        Property propDungMarkPartiesWithMage = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungMarkPartiesWithMage", "do not mark", "Mark parties with Mage class?", new String[]{"always", "if duplicated", "do not mark"}),
                new MooConfigPreview(DataHelper.DungeonClass.MAGE));

        Property propDungMarkPartiesWithTank = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungMarkPartiesWithTank", "do not mark", "Mark parties with Tank class?", new String[]{"always", "if duplicated", "do not mark"}),
                new MooConfigPreview(DataHelper.DungeonClass.TANK));

        Property propDungSendWrongFloorWarning = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungSendWrongFloorWarning", true, "Send warning when entering wrong floor?"));

        boolean modifiedMooCmdAlias = false;
        String mooCmdAliasPreChange = mooCmdAlias;
        boolean modifiedTabCompletableCommandsList = false;
        String[] tabCompletableCommandsPreChange = tabCompletableNamesCommands != null ? tabCompletableNamesCommands.clone() : null;
        boolean modifiedTooltipAuctionHousePriceEachEnchantments = false;
        String[] tooltipAuctionHousePriceEachEnchantmentsPreChange = tooltipAuctionHousePriceEachEnchantments != null ? tooltipAuctionHousePriceEachEnchantments.clone() : null;
        if (readFieldsFromConfig) {
            // Category: General
            configGuiExplanations = propConfigGuiExplanations.getString();
            hasOpenedConfigGui = propHasOpenedConfigGui.getBoolean();
            mooCmdAlias = propMooCmdAlias.getString();
            fixReplyCmd = propFixReplyCmd.getBoolean();
            enableCopyInventory = propEnableCopyInventory.getBoolean();
            wailaLevelOfDetail = propWailaLevelOfDetail.getString();
            copyOrSaveWailaAndInventoryData = propCopyOrSaveWailaAndInventoryData.getString();
            tabCompletableNamesCommands = propTabCompletableNamesCommands.getStringList();
            logsDirs = propLogsDirs.getStringList();
            defaultStartDate = propDefaultStartDate.getString().trim();
            maxLogFileSize = propMaxLogFileSize.getInt();
            // Category: Notifications
            doUpdateCheck = propDoUpdateCheck.getBoolean();
            showBestFriendNotifications = propShowBestFriendNotifications.getBoolean();
            enableBestFriendNotificationSound = propEnableBestFriendNotificationSound.getBoolean();
            showFriendNotifications = propShowFriendNotifications.getBoolean();
            showGuildNotifications = propShowGuildNotifications.getBoolean();
            doBestFriendsOnlineCheck = propDoBestFriendsOnlineCheck.getBoolean();
            // Category: SkyBlock
            enableSkyBlockOnlyFeatures = propEnableSkyBlockOnlyFeatures.getString();
            notifyFreshServer = propNotifyFreshServer.getInt();
            notifyOldServer = propNotifyOldServer.getInt();
            notifyServerAge = propNotifyServerAge.getBoolean();
            chestAnalyzerShowNonBazaarItems = propChestAnalyzerShowNonBazaarItems.getBoolean();
            chestAnalyzerUseBazaarPrices = propChestAnalyzerUseBazaarPrices.getString();
            chestAnalyzerShowCommandUsage = propChestAnalyzerShowCommandUsage.getBoolean();
            tooltipToggleKeyBinding = propTooltipToggleKeyBinding.getInt();
            tooltipItemAge = propTooltipItemAge.getString();
            tooltipItemAgeShortened = propTooltipItemAgeShortened.getBoolean();
            tooltipItemTimestamp = propTooltipItemTimestamp.getString();
            showPetExp = propShowPetExp.getString();
            numeralSystem = propNumeralSystem.getString();
            tooltipAuctionHousePriceEach = propTooltipAuctionHousePriceEach.getString();
            tooltipAuctionHousePriceEachEnchantments = propTooltipAuctionHousePriceEachEnchantments.getStringList();
            auctionHouseMarkEndedAuctions = propAuctionHouseMarkEndedAuctions.getString();
            bazaarSellAllOrder = propBazaarSellAllOrder.getString();
            bazaarSellAllOrderAscDesc = propBazaarSellAllOrderAscDesc.getString();
            bazaarConnectGraphsNodes = propBazaarConnectGraphsNodes.getString();
            bazaarConnectGraphsLineWidth = propBazaarConnectGraphsLineWidth.getInt();
            bestiaryOverviewOrder = propBestiaryOverviewOrder.getString();
            lookupWikiKeyBinding = propLookupWikiKeyBinding.getInt();
            lookupPriceKeyBinding = propLookupPriceKeyBinding.getInt();
            lookupItemDirectly = propLookupItemDirectly.getBoolean();
            // Category: SkyBlock Dungeons
            showItemQualityAndFloor = propShowItemQualityAndFloor.getString();
            dungItemQualityPos = propDungItemQualityPos.getString();
            dungItemQualityShortenNonRandomized = propDungItemQualityShortenNonRandomized.getBoolean();
            dungItemHideGearScore = propDungItemHideGearScore.getBoolean();
            dungItemToolTipToggleKeyBinding = propDungItemToolTipToggleKeyBinding.getInt();
            dungSendPerformanceOnDeath = propDungSendPerformanceOnDeath.getBoolean();
            dungSendPerformanceOnEndScreen = propDungSendPerformanceOnEndScreen.getBoolean();
            dungOverlayEnabled = propDungOverlayEnabled.getBoolean();
            dungOverlayPositionX = propDungOverlayPositionX.getInt();
            dungOverlayPositionY = propDungOverlayPositionY.getInt();
            dungOverlayGuiScale = propDungOverlayGuiScale.getInt();
            dungOverlayTextBorder = propDungOverlayTextBorder.getString();
            dungPartyFinderPlayerLookup = propDungPartyFinderPlayerLookup.getString();
            dungPartyFullLookup = propDungPartyFullLookup.getBoolean();
            dungPartyFinderPartyLookup = propDungPartyFinderPartyLookup.getBoolean();
            dungPartyFinderOverlayDrawBackground = propDungPartyFinderOverlayDrawBackground.getBoolean();
            dungPartiesSize = propDungPartiesSize.getBoolean();
            dungDungeonReqMin = propDungDungeonReqMin.getInt();
            dungClassMin = propDungClassMin.getInt();
            dungMarkPartiesWithCarry = propDungMarkPartiesWithCarry.getString();
            dungMarkPartiesWithHyperion = propDungMarkPartiesWithHyperion.getString();
            dungMarkPartiesWithArcher = propDungMarkPartiesWithArcher.getString();
            dungMarkPartiesWithBerserk = propDungMarkPartiesWithBerserk.getString();
            dungMarkPartiesWithHealer = propDungMarkPartiesWithHealer.getString();
            dungMarkPartiesWithMage = propDungMarkPartiesWithMage.getString();
            dungMarkPartiesWithTank = propDungMarkPartiesWithTank.getString();
            dungSendWrongFloorWarning = propDungSendWrongFloorWarning.getBoolean();

            if (!hasOpenedConfigGui && (!propDungOverlayEnabled.isDefault() || !propDungOverlayPositionX.isDefault() || !propDungOverlayPositionY.isDefault() || !propDungOverlayGuiScale.isDefault())) {
                // player hasn't opened config gui yet and but already moved the dungeon overlay
                hasOpenedConfigGui = true;
            }
            if (!StringUtils.equals(mooCmdAliasPreChange, mooCmdAlias)) {
                modifiedMooCmdAlias = true;
            }
            if (!Arrays.equals(tabCompletableCommandsPreChange, tabCompletableNamesCommands)) {
                modifiedTabCompletableCommandsList = true;
            }
            if (!Arrays.equals(tooltipAuctionHousePriceEachEnchantmentsPreChange, tooltipAuctionHousePriceEachEnchantments)) {
                modifiedTooltipAuctionHousePriceEachEnchantments = true;
            }
        }

        // Category: General
        propConfigGuiExplanations.set(configGuiExplanations);
        propHasOpenedConfigGui.set(hasOpenedConfigGui);
        propMooCmdAlias.set(mooCmdAlias);
        propFixReplyCmd.set(fixReplyCmd);
        propEnableCopyInventory.set(enableCopyInventory);
        propWailaLevelOfDetail.set(wailaLevelOfDetail);
        propCopyOrSaveWailaAndInventoryData.set(copyOrSaveWailaAndInventoryData);
        propTabCompletableNamesCommands.set(tabCompletableNamesCommands);
        propLogsDirs.set(logsDirs);
        propDefaultStartDate.set(defaultStartDate);
        propMaxLogFileSize.set(maxLogFileSize);
        // Category: Notifications
        propDoUpdateCheck.set(doUpdateCheck);
        propShowBestFriendNotifications.set(showBestFriendNotifications);
        propEnableBestFriendNotificationSound.set(enableBestFriendNotificationSound);
        propShowFriendNotifications.set(showFriendNotifications);
        propShowGuildNotifications.set(showGuildNotifications);
        propDoBestFriendsOnlineCheck.set(doBestFriendsOnlineCheck);
        // Category: SkyBlock
        propEnableSkyBlockOnlyFeatures.set(enableSkyBlockOnlyFeatures);
        propNotifyFreshServer.set(notifyFreshServer);
        propNotifyOldServer.set(notifyOldServer);
        propNotifyServerAge.set(notifyServerAge);
        propChestAnalyzerShowNonBazaarItems.set(chestAnalyzerShowNonBazaarItems);
        propChestAnalyzerUseBazaarPrices.set(chestAnalyzerUseBazaarPrices);
        propChestAnalyzerShowCommandUsage.set(chestAnalyzerShowCommandUsage);
        propTooltipToggleKeyBinding.set(tooltipToggleKeyBinding);
        propTooltipItemAge.set(tooltipItemAge);
        propTooltipItemAgeShortened.set(tooltipItemAgeShortened);
        propTooltipItemTimestamp.set(tooltipItemTimestamp);
        propShowPetExp.set(showPetExp);
        propNumeralSystem.set(numeralSystem);
        propTooltipAuctionHousePriceEach.set(tooltipAuctionHousePriceEach);
        propTooltipAuctionHousePriceEachEnchantments.set(tooltipAuctionHousePriceEachEnchantments);
        propAuctionHouseMarkEndedAuctions.set(auctionHouseMarkEndedAuctions);
        propBazaarSellAllOrder.set(bazaarSellAllOrder);
        propBazaarSellAllOrderAscDesc.set(bazaarSellAllOrderAscDesc);
        propBazaarConnectGraphsNodes.set(bazaarConnectGraphsNodes);
        propBazaarConnectGraphsLineWidth.set(bazaarConnectGraphsLineWidth);
        propBestiaryOverviewOrder.set(bestiaryOverviewOrder);
        propLookupWikiKeyBinding.set(lookupWikiKeyBinding);
        propLookupPriceKeyBinding.set(lookupPriceKeyBinding);
        propLookupItemDirectly.set(lookupItemDirectly);
        // Category: SkyBlock Dungeons
        propShowItemQualityAndFloor.set(showItemQualityAndFloor);
        propDungItemQualityPos.set(dungItemQualityPos);
        propDungItemQualityShortenNonRandomized.set(dungItemQualityShortenNonRandomized);
        propDungItemHideGearScore.set(dungItemHideGearScore);
        propDungItemToolTipToggleKeyBinding.set(dungItemToolTipToggleKeyBinding);
        propDungSendPerformanceOnDeath.set(dungSendPerformanceOnDeath);
        propDungSendPerformanceOnEndScreen.set(dungSendPerformanceOnEndScreen);
        propDungOverlayEnabled.set(dungOverlayEnabled);
        propDungOverlayPositionX.set(dungOverlayPositionX);
        propDungOverlayPositionY.set(dungOverlayPositionY);
        propDungOverlayGuiScale.set(dungOverlayGuiScale);
        propDungOverlayTextBorder.set(dungOverlayTextBorder);
        propDungPartyFinderPlayerLookup.set(dungPartyFinderPlayerLookup);
        propDungPartyFullLookup.set(dungPartyFullLookup);
        propDungPartyFinderPartyLookup.set(dungPartyFinderPartyLookup);
        propDungPartyFinderOverlayDrawBackground.set(dungPartyFinderOverlayDrawBackground);
        propDungPartiesSize.set(dungPartiesSize);
        propDungDungeonReqMin.set(dungDungeonReqMin);
        propDungClassMin.set(dungClassMin);
        propDungMarkPartiesWithCarry.set(dungMarkPartiesWithCarry);
        propDungMarkPartiesWithHyperion.set(dungMarkPartiesWithHyperion);
        propDungMarkPartiesWithArcher.set(dungMarkPartiesWithArcher);
        propDungMarkPartiesWithBerserk.set(dungMarkPartiesWithBerserk);
        propDungMarkPartiesWithHealer.set(dungMarkPartiesWithHealer);
        propDungMarkPartiesWithMage.set(dungMarkPartiesWithMage);
        propDungMarkPartiesWithTank.set(dungMarkPartiesWithTank);
        propDungSendWrongFloorWarning.set(dungSendWrongFloorWarning);

        if (saveToFile && cfg.hasChanged()) {
            boolean isPlayerIngame = Minecraft.getMinecraft().thePlayer != null;
            if (modifiedMooCmdAlias) {
                Map<String, ICommand> clientCommandsMap = ClientCommandHandler.instance.getCommands();
                ICommand possibleClientCommand = clientCommandsMap.get(mooCmdAlias);
                if (possibleClientCommand != null && !(possibleClientCommand instanceof MooCommand)) {
                    // tried to use a command name which is already used by another client side command; however, this would overwrite the original command
                    if (isPlayerIngame) {
                        main.getChatHelper().sendMessage(EnumChatFormatting.GOLD, " ⚠ " + EnumChatFormatting.GOLD + "Client-side commands from other mods cannot be used as a command alias. " + EnumChatFormatting.RED + "This would overwrite the other command! Therefore the alias for " + EnumChatFormatting.DARK_RED + "/moo" + EnumChatFormatting.RED + " was not changed to " + EnumChatFormatting.DARK_RED + "/" + mooCmdAlias);
                    }
                    mooCmdAlias = mooCmdAliasPreChange;
                    propMooCmdAlias.set(mooCmdAlias);
                } else if (isPlayerIngame) {
                    if (StringUtils.isEmpty(mooCmdAlias)) {
                        main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Removed command alias for " + EnumChatFormatting.DARK_RED + "/moo " + EnumChatFormatting.RED + "which takes effect after a game restart.");
                    } else {
                        main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Changed command alias for " + EnumChatFormatting.DARK_RED + "/moo " + EnumChatFormatting.RED + "to " + EnumChatFormatting.DARK_RED + "/" + mooCmdAlias + EnumChatFormatting.RED + " which takes effect after a game restart.");
                    }
                }
            }
            if (modifiedTabCompletableCommandsList) {
                if (isPlayerIngame) {
                    main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Added or removed commands with tab-completable usernames take effect after a game restart! If player names cannot be tab-completed for a command after a game restart, check the capitalization of the command name.");
                }
                Map<String, ICommand> clientCommandsMap = ClientCommandHandler.instance.getCommands();
                List<String> removedCommands = new ArrayList<>();
                for (String tabCompletableCommandName : tabCompletableNamesCommands) {
                    ICommand possibleClientCommand = clientCommandsMap.get(tabCompletableCommandName);
                    if (possibleClientCommand != null && !(possibleClientCommand instanceof TabCompletableCommand)) {
                        // tried to add a client side command to tab-completable commands; however, this would overwrite the original command
                        removedCommands.add(tabCompletableCommandName);
                    }
                }
                if (removedCommands.size() > 0) {
                    if (isPlayerIngame) {
                        main.getChatHelper().sendMessage(EnumChatFormatting.GOLD, " ⚠ " + EnumChatFormatting.GOLD + "Client-side commands from other mods cannot be added to commands with tab-completable usernames. " + EnumChatFormatting.RED + "This would overwrite the other command! Therefore the following commands have been removed from the list of commands with tab-completable usernames: " + EnumChatFormatting.GOLD + String.join(EnumChatFormatting.RED + ", " + EnumChatFormatting.GOLD, removedCommands));
                    }
                    tabCompletableNamesCommands = (String[]) ArrayUtils.removeElements(tabCompletableNamesCommands, removedCommands.toArray());
                    propTabCompletableNamesCommands.set(tabCompletableNamesCommands);
                }
            }
            if (modifiedTooltipAuctionHousePriceEachEnchantments) {
                for (int i = 0, enchantmentsLength = tooltipAuctionHousePriceEachEnchantments.length; i < enchantmentsLength; i++) {
                    // standardize enchantment names to match their names in the NBT data
                    String enchantmentName = tooltipAuctionHousePriceEachEnchantments[i];
                    String standardizedEnchantmentName = enchantmentName.toLowerCase().replace(' ', '_');
                    tooltipAuctionHousePriceEachEnchantments[i] = standardizedEnchantmentName;
                }
                propTooltipAuctionHousePriceEachEnchantments.set(tooltipAuctionHousePriceEachEnchantments);
            }

            cfg.save();
        }
    }

    /**
     * Tries to find/resolve default directories containing minecraft logfiles (in .log.gz format)
     *
     * @return list of /logs/ directories
     */
    private String[] resolveDefaultLogsDirs() {
        List<String> logsDirs = new ArrayList<>();
        File currentMcLogsDirFile = new File(Minecraft.getMinecraft().mcDataDir, "logs");
        if (currentMcLogsDirFile.exists() && currentMcLogsDirFile.isDirectory()) {
            String currentMcLogsDir = Utils.toRealPath(currentMcLogsDirFile);
            logsDirs.add(currentMcLogsDir);
        }

        String defaultMcLogsDir = System.getProperty("user.home");
        Util.EnumOS osType = Util.getOSType();
        // default directories for .minecraft: https://minecraft.gamepedia.com/.minecraft
        switch (osType) {
            case WINDOWS:
                defaultMcLogsDir += "\\AppData\\Roaming\\.minecraft\\logs";
                break;
            case OSX:
                defaultMcLogsDir += "/Library/Application Support/minecraft/logs";
                break;
            default:
                defaultMcLogsDir += "/.minecraft/logs";
        }
        File defaultMcLogsDirFile = new File(defaultMcLogsDir);
        if (defaultMcLogsDirFile.exists() && defaultMcLogsDirFile.isDirectory() && !currentMcLogsDirFile.equals(defaultMcLogsDirFile)) {
            logsDirs.add(Utils.toRealPath(defaultMcLogsDirFile));
        }
        return logsDirs.toArray(new String[]{});
    }

    /**
     * @return max log file size in Bytes
     */
    public static long getMaxLogFileSize() {
        return maxLogFileSize * 1024L;
    }

    // Category: General
    public static Setting getConfigGuiExplanationsDisplay() {
        return Setting.get(configGuiExplanations);
    }

    public void theyOpenedTheConfigGui() {
        if (!hasOpenedConfigGui) {
            // opened config gui for the first time!
            main.getChatHelper().sendMessage(new MooChatComponent(EnumChatFormatting.DARK_GREEN + " ❢ " + EnumChatFormatting.GREEN + "To configure " + EnumChatFormatting.GOLD + "Cowlection " + EnumChatFormatting.GREEN + "features use " + EnumChatFormatting.LIGHT_PURPLE + "/moo config").green()
                    .setSuggestCommand("/moo config")
                    .appendFreshSibling(new MooChatComponent("   (this message will be displayed only once)").gray()));
            hasOpenedConfigGui = true;
            syncFromFields();
        }
    }

    public static boolean keepFullWailaInfo() {
        return "all info".equals(wailaLevelOfDetail);
    }

    public static boolean copyWailaAndInventoryDataToClipboard() {
        return "to clipboard".equals(copyOrSaveWailaAndInventoryData);
    }

    // Category: Notifications

    /**
     * Should login/logout notifications be modified and thus monitored?
     *
     * @return true if notifications should be monitored
     */
    public static boolean doMonitorNotifications() {
        return showBestFriendNotifications || enableBestFriendNotificationSound || !showFriendNotifications || !showGuildNotifications;
    }

    // Category: SkyBlock
    public static Setting getEnableSkyBlockOnlyFeatures() {
        return Setting.get(enableSkyBlockOnlyFeatures);
    }

    public static boolean useInstantSellBazaarPrices() {
        return "Instant-Sell".equals(chestAnalyzerUseBazaarPrices);
    }

    public static Setting getTooltipAuctionHousePriceEachDisplay() {
        return Setting.get(tooltipAuctionHousePriceEach);
    }

    public static Setting getMarkAuctionHouseEndedAuctions() {
        return Setting.get(auctionHouseMarkEndedAuctions);
    }

    public static Setting getBazaarConnectGraphsNodes() {
        return Setting.get(bazaarConnectGraphsNodes);
    }

    public static Setting getTooltipItemAgeDisplay() {
        return Setting.get(tooltipItemAge);
    }

    public static Setting getTooltipItemTimestampDisplay() {
        return Setting.get(tooltipItemTimestamp);
    }

    public static Setting getTooltipPetExpDisplay() {
        return Setting.get(showPetExp);
    }

    public static boolean useRomanNumerals() {
        return numeralSystem.startsWith("Roman");
    }

    public void cycleBestiaryOverviewOrder() {
        String oldValue = bestiaryOverviewOrder;
        int currentIndex = -1;
        for (int i = 0; i < bestiaryOverviewOrderDefaultValues.length; i++) {
            if (oldValue.equals(bestiaryOverviewOrderDefaultValues[i])) {
                currentIndex = i;
                break;
            }
        }
        bestiaryOverviewOrder = bestiaryOverviewOrderDefaultValues[(currentIndex + 1) % bestiaryOverviewOrderDefaultValues.length];
        syncFromFields();
    }

    public static boolean isTooltipToggleKeyBindingPressed() {
        return tooltipToggleKeyBinding > 0 && Keyboard.isKeyDown(tooltipToggleKeyBinding);
    }

    public static boolean isLookupWikiKeyBindingPressed() {
        return lookupWikiKeyBinding > 0 && Keyboard.isKeyDown(lookupWikiKeyBinding);
    }

    public static boolean isLookupPriceKeyBindingPressed() {
        return lookupPriceKeyBinding > 0 && Keyboard.isKeyDown(lookupPriceKeyBinding);
    }

    // Category: SkyBlock Dungeons
    public static Setting getShowItemQualityAndFloorDisplay() {
        return Setting.get(showItemQualityAndFloor);
    }

    public static boolean isDungItemQualityAtTop() {
        return dungItemQualityPos.equals("top");
    }

    public static boolean isDungeonItemTooltipToggleKeyBindingPressed() {
        return dungItemToolTipToggleKeyBinding > 0 && Keyboard.isKeyDown(dungItemToolTipToggleKeyBinding);
    }

    public static Setting getDungOverlayTextBorder() {
        switch (dungOverlayTextBorder) {
            case "drop shadow":
                return Setting.TEXT;
            case "full outline":
                return Setting.ALWAYS;
            default:
                // everything else: "no border"
                return Setting.DISABLED;
        }
    }

    public static Setting getDungPartyFinderPlayerLookupDisplay() {
        return Setting.get(dungPartyFinderPlayerLookup);
    }

    public static DataHelper.PartyType getDungPartyFinderMarkCarry() {
        return getPartyType(dungMarkPartiesWithCarry);
    }

    public static DataHelper.PartyType getDungPartyFinderMarkHyperion() {
        return getPartyType(dungMarkPartiesWithHyperion);
    }

    private static DataHelper.PartyType getPartyType(String configValue) {
        String configValueBeginning = configValue.length() >= 5 ? configValue.substring(0, 5) : "invalid";
        switch (configValueBeginning) {
            case "suita": // "suitable " + EnumChatFormatting.GREEN + "⬛"
                return DataHelper.PartyType.SUITABLE;
            case "unide": // "unideal " + EnumChatFormatting.GOLD + "⬛"
                return DataHelper.PartyType.UNIDEAL;
            case "block": // "block " + EnumChatFormatting.RED + "⬛"
                return DataHelper.PartyType.UNJOINABLE;
            default: // "do not mark"
                return DataHelper.PartyType.NONE;
        }
    }

    public static Setting filterDungPartiesWithDupes(DataHelper.DungeonClass dungeonClass) {
        String setting;
        switch (dungeonClass) {
            case ARCHER:
                setting = dungMarkPartiesWithArcher;
                break;
            case BERSERK:
                setting = dungMarkPartiesWithBerserk;
                break;
            case HEALER:
                setting = dungMarkPartiesWithHealer;
                break;
            case MAGE:
                setting = dungMarkPartiesWithMage;
                break;
            case TANK:
                setting = dungMarkPartiesWithTank;
                break;
            default:
                setting = "do not mark";
                break;
        }
        switch (setting) {
            case "always":
                return Setting.ALWAYS;
            case "if duplicated":
                return Setting.SPECIAL;
            default: // do not mark
                return Setting.DISABLED;
        }
    }

    // MC Log Search:
    public static LocalDate calculateStartDate() {
        try {
            // date format: yyyy-mm-dd
            return LocalDate.parse(defaultStartDate);
        } catch (DateTimeParseException e) {
            // fall-through
        }
        try {
            int months = Integer.parseInt(defaultStartDate);
            return LocalDate.now().minus(months, ChronoUnit.MONTHS);
        } catch (NumberFormatException e) {
            // default: 1 month
            return LocalDate.now().minus(1, ChronoUnit.MONTHS);
        }
    }

    // other stuff
    public static List<MooConfigCategory> getConfigCategories() {
        return configCategories;
    }

    public Property getMooCmdAliasProperty() {
        return propMooCmdAlias;
    }

    public Property getTabCompletableNamesCommandsProperty() {
        return propTabCompletableNamesCommands;
    }

    public List<Property> getLogSearchProperties() {
        return logSearchProperties;
    }

    public Property getTooltipAuctionHousePriceEachEnchantmentsProperty() {
        return propTooltipAuctionHousePriceEachEnchantments;
    }


    public class ConfigEventHandler {
        @SubscribeEvent(priority = EventPriority.NORMAL)
        public void onEvent(ConfigChangedEvent.OnConfigChangedEvent e) {
            if (Cowlection.MODID.equals(e.modID)) {
                syncFromGui();
            }
        }
    }

    public enum Setting {
        UNKNOWN, DISABLED, //
        ALWAYS, TOOLTIP, TEXT, //
        SPECIAL;

        public static Setting get(String configValue) {
            switch (configValue) {
                case "always":
                    return ALWAYS;
                case "as a tooltip":
                case "as tooltip":
                    return TOOLTIP;
                case "in chat":
                case "as text":
                case "a word":
                    return TEXT;
                case "hidden":
                case "never":
                case "disabled":
                    return DISABLED;
                case "on SkyBlock":
                case "key press":
                case "a letter":
                    return SPECIAL;
                default:
                    return UNKNOWN;
            }
        }
    }
}
