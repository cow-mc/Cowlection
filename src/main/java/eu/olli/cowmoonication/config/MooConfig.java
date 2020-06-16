package eu.olli.cowmoonication.config;

import eu.olli.cowmoonication.Cowmoonication;
import eu.olli.cowmoonication.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Util;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.FMLConfigGuiFactory;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Mod configuration via ingame gui
 * <p>
 * Based on <a href="https://github.com/TheGreyGhost/MinecraftByExample/blob/1-8-9final/src/main/java/minecraftbyexample/mbe70_configuration/MBEConfiguration.java">TheGreyGhost's MinecraftByExample</a>
 *
 * @see ForgeModContainer
 * @see FMLConfigGuiFactory
 */
public class MooConfig {
    public static final String CATEGORY_LOGS_SEARCH = "logssearch";
    // main config
    public static boolean doUpdateCheck;
    public static boolean showBestFriendNotifications;
    public static boolean showFriendNotifications;
    public static boolean showGuildNotifications;
    public static String[] tabCompletableNamesCommands;
    // logs search config
    public static String[] logsDirs;
    private static String defaultStartDate;
    // other stuff
    public static String moo;
    private static Configuration cfg = null;
    private final Cowmoonication main;
    private List<String> propOrderGeneral;
    private List<String> propOrderLogsSearch;

    public MooConfig(Cowmoonication main, Configuration configuration) {
        this.main = main;
        cfg = configuration;
        initConfig();
    }

    static Configuration getConfig() {
        return cfg;
    }

    private void initConfig() {
        syncFromFile();
        MinecraftForge.EVENT_BUS.register(new ConfigEventHandler());
    }

    /**
     * Load the configuration values from the configuration file
     */
    private void syncFromFile() {
        syncConfig(true, true);
    }

    /**
     * Save the GUI-altered values to disk
     */
    private void syncFromGui() {
        syncConfig(false, true);
    }

    /**
     * Save the Configuration variables (fields) to disk
     */
    public void syncFromFields() {
        syncConfig(false, false);
    }

    public static LocalDate calculateStartDate() {
        try {
            // date format: yyyy-mm-dd
            return LocalDate.parse(defaultStartDate);
        } catch (DateTimeParseException e) {
            // fallthrough
        }
        try {
            int months = Integer.parseInt(defaultStartDate);
            return LocalDate.now().minus(months, ChronoUnit.MONTHS);
        } catch (NumberFormatException e) {
            // default: 1 month
            return LocalDate.now().minus(1, ChronoUnit.MONTHS);
        }
    }

    /**
     * Synchronise the three copies of the data
     * 1) loadConfigFromFile && readFieldsFromConfig -> initialise everything from the disk file
     * 2) !loadConfigFromFile && readFieldsFromConfig -> copy everything from the config file (altered by GUI)
     * 3) !loadConfigFromFile && !readFieldsFromConfig -> copy everything from the native fields
     *
     * @param loadConfigFromFile   if true, load the config field from the configuration file on disk
     * @param readFieldsFromConfig if true, reload the member variables from the config field
     */
    private void syncConfig(boolean loadConfigFromFile, boolean readFieldsFromConfig) {
        if (loadConfigFromFile) {
            cfg.load();
        }

        // config section: main configuration
        propOrderGeneral = new ArrayList<>();

        Property propDoUpdateCheck = addConfigEntry(cfg.get(Configuration.CATEGORY_CLIENT,
                "doUpdateCheck", true, "Check for mod updates?"), true);
        Property propShowBestFriendNotifications = addConfigEntry(cfg.get(Configuration.CATEGORY_CLIENT,
                "showBestFriendNotifications", true, "Set to true to receive best friends' login/logout messages, set to false hide them."), true);
        Property propShowFriendNotifications = addConfigEntry(cfg.get(Configuration.CATEGORY_CLIENT,
                "showFriendNotifications", false, "Set to true to receive friends' login/logout messages, set to false hide them."), true);
        Property propShowGuildNotifications = addConfigEntry(cfg.get(Configuration.CATEGORY_CLIENT,
                "showGuildNotifications", false, "Set to true to receive guild members' login/logout messages, set to false hide them."), true);
        Property propTabCompletableNamesCommands = addConfigEntry(cfg.get(Configuration.CATEGORY_CLIENT,
                "tabCompletableNamesCommands", new String[]{"party", "p", "invite", "visit", "ignore", "msg", "tell", "w", "boop", "profile"}, "List of commands with a Tab-completable username argument."), true)
                .setValidationPattern(Pattern.compile("^[A-Za-z]+$"));
        Property propMoo = addConfigEntry(cfg.get(Configuration.CATEGORY_CLIENT,
                "moo", "", "The answer to life the universe and everything. Don't edit this entry manually!", Utils.VALID_UUID_PATTERN), false);

        cfg.setCategoryPropertyOrder(Configuration.CATEGORY_CLIENT, propOrderGeneral);

        // config section: log files search
        propOrderLogsSearch = new ArrayList<>();

        Property propLogsDirs = addConfigEntry(cfg.get(CATEGORY_LOGS_SEARCH,
                "logsDirs", resolveDefaultLogsDirs(),
                "Directories with Minecraft log files"), true, CATEGORY_LOGS_SEARCH);
        Property propDefaultStartDate = addConfigEntry(cfg.get(CATEGORY_LOGS_SEARCH,
                "defaultStartDate", "3", "Default start date (a number means X months ago, alternatively a fixed date à la yyyy-mm-dd can be used)"), true)
                .setValidationPattern(Pattern.compile("^[1-9][0-9]{0,2}|(2[0-9]{3}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01]))$"));

        cfg.setCategoryPropertyOrder(CATEGORY_LOGS_SEARCH, propOrderLogsSearch);

        // 'manual' replacement for propTabCompletableNamesCommands.hasChanged()
        boolean modifiedTabCompletableCommandsList = false;
        String[] tabCompletableCommandsPreChange = tabCompletableNamesCommands != null ? tabCompletableNamesCommands.clone() : null;

        if (readFieldsFromConfig) {
            // main config
            doUpdateCheck = propDoUpdateCheck.getBoolean();
            showBestFriendNotifications = propShowBestFriendNotifications.getBoolean();
            showFriendNotifications = propShowFriendNotifications.getBoolean();
            showGuildNotifications = propShowGuildNotifications.getBoolean();
            tabCompletableNamesCommands = propTabCompletableNamesCommands.getStringList();
            moo = propMoo.getString();

            // logs search config
            logsDirs = propLogsDirs.getStringList();
            defaultStartDate = propDefaultStartDate.getString().trim();

            if (!Arrays.equals(tabCompletableCommandsPreChange, tabCompletableNamesCommands)) {
                modifiedTabCompletableCommandsList = true;
            }
        }

        // main config
        propDoUpdateCheck.set(doUpdateCheck);
        propShowBestFriendNotifications.set(showBestFriendNotifications);
        propShowFriendNotifications.set(showFriendNotifications);
        propShowGuildNotifications.set(showGuildNotifications);
        propTabCompletableNamesCommands.set(tabCompletableNamesCommands);
        propMoo.set(moo);

        // logs search config
        propLogsDirs.set(logsDirs);
        propDefaultStartDate.set(defaultStartDate);

        if (cfg.hasChanged()) {
            if (modifiedTabCompletableCommandsList && Minecraft.getMinecraft().thePlayer != null) {
                main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Added or removed commands with tab-completable usernames take effect after a game restart!");
            }
            cfg.save();
        }
    }

    private Property addConfigEntry(Property property, boolean showInGui, String category) {
        if (showInGui) {
            property.setLanguageKey(Cowmoonication.MODID + ".config." + property.getName());
        } else {
            property.setShowInGui(false);
        }

        if (CATEGORY_LOGS_SEARCH.equals(category)) {
            propOrderLogsSearch.add(property.getName());
        } else {
            // == Configuration.CATEGORY_CLIENT:
            propOrderGeneral.add(property.getName());
        }
        return property;
    }

    private Property addConfigEntry(Property property, boolean showInGui) {
        return addConfigEntry(property, showInGui, Configuration.CATEGORY_CLIENT);
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
            String currentMcLogsDir = currentMcLogsDirFile.getAbsolutePath();
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
            logsDirs.add(defaultMcLogsDirFile.getAbsolutePath());
        }
        return logsDirs.toArray(new String[]{});
    }

    /**
     * Should login/logout notifications be modified and thus monitored?
     *
     * @return true if notifications should be monitored
     */
    public static boolean doMonitorNotifications() {
        return showBestFriendNotifications || !showFriendNotifications || !showGuildNotifications;
    }

    public class ConfigEventHandler {
        @SubscribeEvent(priority = EventPriority.NORMAL)
        public void onEvent(ConfigChangedEvent.OnConfigChangedEvent e) {
            if (Cowmoonication.MODID.equals(e.modID)) {
                syncFromGui();
            }
        }
    }
}
