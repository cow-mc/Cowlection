package eu.olli.cowmoonication.config;

import eu.olli.cowmoonication.Cowmoonication;
import eu.olli.cowmoonication.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class MooConfig {
    public static boolean doUpdateCheck;
    public static boolean showBestFriendNotifications;
    public static boolean showFriendNotifications;
    public static boolean showGuildNotifications;
    public static String[] tabCompletableNamesCommands;
    public static String moo;
    private static Configuration cfg = null;
    private final Cowmoonication main;
    private List<String> propOrderGeneral;

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
    private void syncFromGUI() {
        syncConfig(false, true);
    }

    /**
     * Save the Configuration variables (fields) to disk
     */
    public void syncFromFields() {
        syncConfig(false, false);
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

        // 'manual' replacement for propTabCompletableNamesCommands.hasChanged()
        boolean modifiedTabCompletableCommandsList = false;
        String[] tabCompletableCommandsPreChange = tabCompletableNamesCommands != null ? tabCompletableNamesCommands.clone() : null;

        if (readFieldsFromConfig) {
            doUpdateCheck = propDoUpdateCheck.getBoolean();
            showBestFriendNotifications = propShowBestFriendNotifications.getBoolean();
            showFriendNotifications = propShowFriendNotifications.getBoolean();
            showGuildNotifications = propShowGuildNotifications.getBoolean();
            tabCompletableNamesCommands = propTabCompletableNamesCommands.getStringList();
            moo = propMoo.getString();

            if (!Arrays.equals(tabCompletableCommandsPreChange, tabCompletableNamesCommands)) {
                modifiedTabCompletableCommandsList = true;
            }
        }

        propDoUpdateCheck.set(doUpdateCheck);
        propShowBestFriendNotifications.set(showBestFriendNotifications);
        propShowFriendNotifications.set(showFriendNotifications);
        propShowGuildNotifications.set(showGuildNotifications);
        propTabCompletableNamesCommands.set(tabCompletableNamesCommands);
        propMoo.set(moo);

        if (cfg.hasChanged()) {
            if (modifiedTabCompletableCommandsList && Minecraft.getMinecraft().thePlayer != null) {
                main.getChatHelper().sendMessage(EnumChatFormatting.RED, "Added or removed commands with tab-completable usernames take effect after a game restart!");
            }
            cfg.save();
        }
    }

    private Property addConfigEntry(Property property, boolean showInGui) {
        if (showInGui) {
            property.setLanguageKey(Cowmoonication.MODID + ".config." + property.getName());
        } else {
            property.setShowInGui(false);
        }
        propOrderGeneral.add(property.getName());
        return property;
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
                syncFromGUI();
            }
        }
    }
}
