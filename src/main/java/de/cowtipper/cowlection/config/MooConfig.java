package de.cowtipper.cowlection.config;

import de.cowtipper.cowlection.Cowlection;
import de.cowtipper.cowlection.command.MooCommand;
import de.cowtipper.cowlection.command.TabCompletableCommand;
import de.cowtipper.cowlection.config.gui.MooConfigGui;
import de.cowtipper.cowlection.config.gui.MooConfigPreview;
import de.cowtipper.cowlection.data.DataHelper;
import de.cowtipper.cowlection.util.MooChatComponent;
import de.cowtipper.cowlection.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommand;
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
    public static String mooCmdAlias;
    public static boolean fixReplyCmd;
    public static boolean enableCopyInventory;
    public static String[] tabCompletableNamesCommands;
    private static final String CATEGORY_LOGS_SEARCH = "logssearch";
    public static String[] logsDirs;
    private static String defaultStartDate;
    // Category: Notifications
    public static boolean doUpdateCheck;
    public static boolean showBestFriendNotifications;
    public static boolean showFriendNotifications;
    public static boolean showGuildNotifications;
    public static boolean doBestFriendsOnlineCheck;
    // Category: SkyBlock
    private static String enableSkyBlockOnlyFeatures;
    public static int tooltipToggleKeyBinding;
    private static String tooltipAuctionHousePriceEach;
    private static String tooltipItemAge;
    public static boolean tooltipItemAgeShortened;
    private static String tooltipItemTimestamp;
    private static String numeralSystem;
    // Category: SkyBlock Dungeons
    private static String showItemQualityAndFloor;
    private static String dungItemQualityPos;
    public static int dungItemToolTipToggleKeyBinding;
    public static boolean dungOverlayEnabled;
    public static int dungOverlayPositionX;
    public static int dungOverlayPositionY;
    public static int dungOverlayGuiScale;
    public static boolean dungOverlayTextShadow;
    public static int dungClassMin;
    public static boolean dungFilterPartiesWithArcherDupes;
    public static boolean dungFilterPartiesWithBerserkDupes;
    public static boolean dungFilterPartiesWithHealerDupes;
    public static boolean dungFilterPartiesWithMageDupes;
    public static boolean dungFilterPartiesWithTankDupes;
    private static String dungPartyFinderPlayerLookup;

    private static Configuration cfg = null;
    private static final List<MooConfigCategory> configCategories = new ArrayList<>();
    private final Cowlection main;
    private Property propMooCmdAlias;
    private Property propTabCompletableNamesCommands;
    private List<Property> logSearchProperties;

    public MooConfig(Cowlection main, Configuration configuration) {
        this.main = main;
        cfg = configuration;

        if (cfg.getLoadedConfigVersion() == null || !cfg.getLoadedConfigVersion().equals(cfg.getDefinedConfigVersion())) {
            updateConfig(cfg.getLoadedConfigVersion());
        }

        initConfig();
    }

    private void updateConfig(String oldVersion) {
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
        subCat.addExplanations("Display of the explanations for each sub-section:",
                "  ‣ " + EnumChatFormatting.YELLOW + "as tooltip ①" + EnumChatFormatting.DARK_GRAY + "⬛" + EnumChatFormatting.RESET + " = tooltip when hovering over sub-category heading (with darkened background)",
                "  ‣ " + EnumChatFormatting.YELLOW + "as tooltip ②" + EnumChatFormatting.WHITE + "⬛" + EnumChatFormatting.RESET + " = tooltip when hovering over sub-category heading (no extra background)",
                "  ‣ " + EnumChatFormatting.YELLOW + "as text" + EnumChatFormatting.RESET + " = below each sub-category heading",
                "  ‣ " + EnumChatFormatting.YELLOW + "hidden" + EnumChatFormatting.RESET + " = ");
        Property propConfigGuiExplanations = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "configGuiExplanations", "tooltip ① §0⬛", "Display config settings explanations",
                new String[]{"as tooltip ①§0⬛", "as tooltip ②§f⬛", "as text", "hidden"}));

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

        // Sub-Category: Tab-completable names in commands
        subCat = configCat.addSubCategory("Tab-completable usernames");
        subCat.addExplanations("For certain commands you can use " + EnumChatFormatting.YELLOW + "TAB " + EnumChatFormatting.RESET + "to autocomplete player names",
                EnumChatFormatting.UNDERLINE + "Uses player names from:",
                "  ‣ Guild and Party chat",
                "  ‣ Party and game (duels) invites",
                "  ‣ SkyBlock Dungeon party finder: when a player joins the group",
                "  ‣ Online best friends (if the best friend online checker is enabled)");

        propTabCompletableNamesCommands = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "tabCompletableNamesCommands", new String[]{"party", "p", "invite", "visit", "ah", "ignore", "msg", "tell", "w", "boop", "profile", "friend", "friends"}, "List of commands with a Tab-completable username argument.")
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
        logSearchProperties = new ArrayList<>();
        logSearchProperties.add(propLogsDirs);
        logSearchProperties.add(propDefaultStartDate);

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

        // Sub-Category: Tooltip enhancements
        subCat = configCat.addSubCategory("Tooltip enhancements");

        Property propTooltipToggleKeyBinding = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "tooltipToggleKeyBinding", Keyboard.KEY_LSHIFT, "Key to toggle tooltip"));

        Property propTooltipAuctionHousePriceEach = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "tooltipAuctionHousePriceEach", "always", "Add price per item if multiple items are bought or sold", new String[]{"always", "key press", "never"}));

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

        Property propNumeralSystem = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "numeralSystem", "Arabic: 1, 4, 10", "Use Roman or Arabic numeral system?", new String[]{"Arabic: 1, 4, 10", "Roman: I, IV, X"}));


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

        Property propDungItemToolTipToggleKeyBinding = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungItemToolTipToggleKeyBinding", Keyboard.KEY_LSHIFT, "Key to toggle dungeon item tooltip"));

        // Sub-Category: Performance Overlay
        subCat = configCat.addSubCategory("Performance Overlay");
        subCat.addExplanations(EnumChatFormatting.UNDERLINE + "Keeps track of:",
                "  ‣ skill score " + EnumChatFormatting.GRAY + "(reduced by deaths and failed puzzles)",
                "  ‣ speed score " + EnumChatFormatting.GRAY + "(-2.2 points when over 20 minutes)",
                "  ‣ bonus score " + EnumChatFormatting.GRAY + "(+1 [max 5] for each destroyed crypt; can only be detected up to ~50 blocks away from the player)",
                "Does " + EnumChatFormatting.ITALIC + "not" + EnumChatFormatting.RESET + " track explorer score " + EnumChatFormatting.GRAY + "(explored rooms, secrets, ...)");

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

        Property propDungOverlayTextShadow = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungOverlayTextShadow", true, "Dungeon performance overlay GUI scale"));

        // Sub-Category: Party Finder
        subCat = configCat.addSubCategory("Dungeon Party Finder");
        subCat.addExplanations("Adds various indicators to the dungeon party finder",
                "to make it easier to find the perfect party:",
                "",
                "Marks parties...",
                "  ‣ you cannot join: " + EnumChatFormatting.RED + "⬛" + EnumChatFormatting.RESET + " (red carpet)",
                "  ‣ with someone below a certain class level: " + EnumChatFormatting.RED + EnumChatFormatting.BOLD + "ᐯ" + EnumChatFormatting.RESET,
                "  ‣ with duplicated roles you specify below: " + EnumChatFormatting.GOLD + "²⁺",
                "  ‣ that match your criteria: " + EnumChatFormatting.GREEN + "⬛" + EnumChatFormatting.RESET + " (green carpet)");

        Property propDungClassMin = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungClassMin", 0, "Marks parties with members with lower class level than this value")
                        .setMinValue(0).setMaxValue(50),
                new MooConfigPreview(new MooChatComponent("Marked with: " + EnumChatFormatting.RED + EnumChatFormatting.BOLD + "ᐯ").gray()));

        Property propDungFilterPartiesWithArcherDupes = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungFilterPartiesWithArcherDupes", true, "Mark parties with duplicated Archer class?"),
                new MooConfigPreview(new MooChatComponent("Marked with: " + EnumChatFormatting.GOLD + "²⁺A").gray()));

        Property propDungFilterPartiesWithBerserkDupes = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungFilterPartiesWithBerserkDupes", false, "Mark parties with duplicated Berserk class?"),
                new MooConfigPreview(new MooChatComponent("Marked with: " + EnumChatFormatting.GOLD + "²⁺B").gray()));

        Property propDungFilterPartiesWithHealerDupes = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungFilterPartiesWithHealerDupes", false, "Mark parties with duplicated Healer class?"),
                new MooConfigPreview(new MooChatComponent("Marked with: " + EnumChatFormatting.GOLD + "²⁺H").gray()));

        Property propDungFilterPartiesWithMageDupes = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungFilterPartiesWithMageDupes", false, "Mark parties with duplicated Mage class?"),
                new MooConfigPreview(new MooChatComponent("Marked with: " + EnumChatFormatting.GOLD + "²⁺M").gray()));

        Property propDungFilterPartiesWithTankDupes = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungFilterPartiesWithTankDupes", false, "Mark parties with duplicated Tank class?"),
                new MooConfigPreview(new MooChatComponent("Marked with: " + EnumChatFormatting.GOLD + "²⁺T").gray()));

        Property propDungPartyFinderPlayerLookup = subCat.addConfigEntry(cfg.get(configCat.getConfigName(),
                "dungPartyFinderPlayerLookup", "as a tooltip", "Show armor + dungeons stats of player joining via party finder as a tooltip or in chat?", new String[]{"as a tooltip", "in chat", "disabled"}));

        boolean modifiedMooCmdAlias = false;
        String mooCmdAliasPreChange = mooCmdAlias;
        boolean modifiedTabCompletableCommandsList = false;
        String[] tabCompletableCommandsPreChange = tabCompletableNamesCommands != null ? tabCompletableNamesCommands.clone() : null;
        if (readFieldsFromConfig) {
            // Category: General
            configGuiExplanations = propConfigGuiExplanations.getString();
            mooCmdAlias = propMooCmdAlias.getString();
            fixReplyCmd = propFixReplyCmd.getBoolean();
            enableCopyInventory = propEnableCopyInventory.getBoolean();
            tabCompletableNamesCommands = propTabCompletableNamesCommands.getStringList();
            logsDirs = propLogsDirs.getStringList();
            defaultStartDate = propDefaultStartDate.getString().trim();
            // Category: Notifications
            doUpdateCheck = propDoUpdateCheck.getBoolean();
            showBestFriendNotifications = propShowBestFriendNotifications.getBoolean();
            showFriendNotifications = propShowFriendNotifications.getBoolean();
            showGuildNotifications = propShowGuildNotifications.getBoolean();
            doBestFriendsOnlineCheck = propDoBestFriendsOnlineCheck.getBoolean();
            // Category: SkyBlock
            enableSkyBlockOnlyFeatures = propEnableSkyBlockOnlyFeatures.getString();
            tooltipToggleKeyBinding = propTooltipToggleKeyBinding.getInt();
            tooltipAuctionHousePriceEach = propTooltipAuctionHousePriceEach.getString();
            tooltipItemAge = propTooltipItemAge.getString();
            tooltipItemAgeShortened = propTooltipItemAgeShortened.getBoolean();
            tooltipItemTimestamp = propTooltipItemTimestamp.getString();
            numeralSystem = propNumeralSystem.getString();
            // Category: SkyBlock Dungeons
            showItemQualityAndFloor = propShowItemQualityAndFloor.getString();
            dungItemQualityPos = propDungItemQualityPos.getString();
            dungItemToolTipToggleKeyBinding = propDungItemToolTipToggleKeyBinding.getInt();
            dungOverlayEnabled = propDungOverlayEnabled.getBoolean();
            dungOverlayPositionX = propDungOverlayPositionX.getInt();
            dungOverlayPositionY = propDungOverlayPositionY.getInt();
            dungOverlayGuiScale = propDungOverlayGuiScale.getInt();
            dungOverlayTextShadow = propDungOverlayTextShadow.getBoolean();
            dungClassMin = propDungClassMin.getInt();
            dungFilterPartiesWithArcherDupes = propDungFilterPartiesWithArcherDupes.getBoolean();
            dungFilterPartiesWithBerserkDupes = propDungFilterPartiesWithBerserkDupes.getBoolean();
            dungFilterPartiesWithHealerDupes = propDungFilterPartiesWithHealerDupes.getBoolean();
            dungFilterPartiesWithMageDupes = propDungFilterPartiesWithMageDupes.getBoolean();
            dungFilterPartiesWithTankDupes = propDungFilterPartiesWithTankDupes.getBoolean();
            dungPartyFinderPlayerLookup = propDungPartyFinderPlayerLookup.getString();


            if (!StringUtils.equals(mooCmdAliasPreChange, mooCmdAlias)) {
                modifiedMooCmdAlias = true;
            }
            if (!Arrays.equals(tabCompletableCommandsPreChange, tabCompletableNamesCommands)) {
                modifiedTabCompletableCommandsList = true;
            }
        }

        // Category: General
        propConfigGuiExplanations.set(configGuiExplanations);
        propMooCmdAlias.set(mooCmdAlias);
        propFixReplyCmd.set(fixReplyCmd);
        propEnableCopyInventory.set(enableCopyInventory);
        propTabCompletableNamesCommands.set(tabCompletableNamesCommands);
        propLogsDirs.set(logsDirs);
        propDefaultStartDate.set(defaultStartDate);
        // Category: Notifications
        propDoUpdateCheck.set(doUpdateCheck);
        propShowBestFriendNotifications.set(showBestFriendNotifications);
        propShowFriendNotifications.set(showFriendNotifications);
        propShowGuildNotifications.set(showGuildNotifications);
        propDoBestFriendsOnlineCheck.set(doBestFriendsOnlineCheck);
        // Category: SkyBlock
        propEnableSkyBlockOnlyFeatures.set(enableSkyBlockOnlyFeatures);
        propTooltipToggleKeyBinding.set(tooltipToggleKeyBinding);
        propTooltipAuctionHousePriceEach.set(tooltipAuctionHousePriceEach);
        propTooltipItemAge.set(tooltipItemAge);
        propTooltipItemAgeShortened.set(tooltipItemAgeShortened);
        propTooltipItemTimestamp.set(tooltipItemTimestamp);
        propNumeralSystem.set(numeralSystem);
        // Category: SkyBlock Dungeons
        propShowItemQualityAndFloor.set(showItemQualityAndFloor);
        propDungItemQualityPos.set(dungItemQualityPos);
        propDungItemToolTipToggleKeyBinding.set(dungItemToolTipToggleKeyBinding);
        propDungOverlayEnabled.set(dungOverlayEnabled);
        propDungOverlayPositionX.set(dungOverlayPositionX);
        propDungOverlayPositionY.set(dungOverlayPositionY);
        propDungOverlayGuiScale.set(dungOverlayGuiScale);
        propDungOverlayTextShadow.set(dungOverlayTextShadow);
        propDungClassMin.set(dungClassMin);
        propDungFilterPartiesWithArcherDupes.set(dungFilterPartiesWithArcherDupes);
        propDungFilterPartiesWithBerserkDupes.set(dungFilterPartiesWithBerserkDupes);
        propDungFilterPartiesWithHealerDupes.set(dungFilterPartiesWithHealerDupes);
        propDungFilterPartiesWithMageDupes.set(dungFilterPartiesWithMageDupes);
        propDungFilterPartiesWithTankDupes.set(dungFilterPartiesWithTankDupes);
        propDungPartyFinderPlayerLookup.set(dungPartyFinderPlayerLookup);

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

    // Category: General
    public static Setting getConfigGuiExplanationsDisplay() {
        return Setting.get(configGuiExplanations);
    }

    // Category: Notifications

    /**
     * Should login/logout notifications be modified and thus monitored?
     *
     * @return true if notifications should be monitored
     */
    public static boolean doMonitorNotifications() {
        return showBestFriendNotifications || !showFriendNotifications || !showGuildNotifications;
    }

    // Category: SkyBlock
    public static Setting getEnableSkyBlockOnlyFeatures() {
        return Setting.get(MooConfig.enableSkyBlockOnlyFeatures);
    }

    public static Setting getTooltipAuctionHousePriceEachDisplay() {
        return Setting.get(tooltipAuctionHousePriceEach);
    }

    public static Setting getTooltipItemAgeDisplay() {
        return Setting.get(tooltipItemAge);
    }

    public static Setting getTooltipItemTimestampDisplay() {
        return Setting.get(tooltipItemTimestamp);
    }

    public static boolean useRomanNumerals() {
        return numeralSystem.startsWith("Roman");
    }

    public static boolean isTooltipToggleKeyBindingPressed() {
        return tooltipToggleKeyBinding > 0 && Keyboard.isKeyDown(MooConfig.tooltipToggleKeyBinding);
    }

    public static boolean isDungeonItemTooltipToggleKeyBindingPressed() {
        return dungItemToolTipToggleKeyBinding > 0 && Keyboard.isKeyDown(MooConfig.dungItemToolTipToggleKeyBinding);
    }

    // Category: SkyBlock Dungeons
    public static Setting getShowItemQualityAndFloorDisplay() {
        return Setting.get(showItemQualityAndFloor);
    }

    public static boolean isDungItemQualityAtTop() {
        return dungItemQualityPos.equals("top");
    }

    public static Setting getDungPartyFinderPlayerLookupDisplay() {
        return Setting.get(dungPartyFinderPlayerLookup);
    }

    public static boolean filterDungPartiesWithDupes(DataHelper.DungeonClass dungeonClass) {
        switch (dungeonClass) {
            case ARCHER:
                return dungFilterPartiesWithArcherDupes;
            case BERSERK:
                return dungFilterPartiesWithBerserkDupes;
            case HEALER:
                return dungFilterPartiesWithHealerDupes;
            case MAGE:
                return dungFilterPartiesWithMageDupes;
            case TANK:
                return dungFilterPartiesWithTankDupes;
            default:
                return false;
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
                case "as tooltip ②§f⬛":
                    return TOOLTIP;
                case "in chat":
                case "as text":
                    return TEXT;
                case "hidden":
                case "never":
                case "disabled":
                    return DISABLED;
                case "on SkyBlock":
                case "key press":
                case "as tooltip ①§0⬛":
                    return SPECIAL;
                default:
                    return UNKNOWN;
            }
        }
    }
}
