package eu.olli.cowmoonication.config;

import eu.olli.cowmoonication.Cowmoonication;
import eu.olli.cowmoonication.util.Utils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class MooConfig {
    public static boolean doUpdateCheck;
    public static boolean showBestFriendNotifications;
    public static boolean showFriendNotifications;
    public static boolean showGuildNotifications;
    public static String moo;
    private static Configuration cfg = null;

    public MooConfig(Configuration configuration) {
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

        final boolean DO_UPDATE_CHECK = true;
        Property propDoUpdateCheck = cfg.get(Configuration.CATEGORY_CLIENT, "doUpdateCheck", DO_UPDATE_CHECK, "Check for mod updates?");

        final boolean SHOW_BEST_FRIEND_NOTIFICATIONS = true;
        Property propShowBestFriendNotifications = cfg.get(Configuration.CATEGORY_CLIENT, "showBestFriendNotifications", SHOW_BEST_FRIEND_NOTIFICATIONS, "Set to true to receive best friends' login/logout messages, set to false hide them.");

        final boolean SHOW_FRIEND_NOTIFICATIONS = false;
        Property propShowFriendNotifications = cfg.get(Configuration.CATEGORY_CLIENT, "showFriendNotifications", SHOW_FRIEND_NOTIFICATIONS, "Set to true to receive friends' login/logout messages, set to false hide them.");

        final boolean SHOW_GUILD_NOTIFICATIONS = false;
        Property propShowGuildNotifications = cfg.get(Configuration.CATEGORY_CLIENT, "showGuildNotifications", SHOW_GUILD_NOTIFICATIONS, "Set to true to receive guild members' login/logout messages, set to false hide them.");

        final String MOO = "";
        Property propMoo = cfg.get(Configuration.CATEGORY_CLIENT, "moo", MOO, "The answer to life the universe and everything. Don't edit this entry manually!", Utils.VALID_UUID_PATTERN);
        propMoo.setShowInGui(false);

        List<String> propOrderGeneral = new ArrayList<>();
        propOrderGeneral.add(propDoUpdateCheck.getName());
        propOrderGeneral.add(propShowBestFriendNotifications.getName());
        propOrderGeneral.add(propShowFriendNotifications.getName());
        propOrderGeneral.add(propShowGuildNotifications.getName());
        propOrderGeneral.add(propMoo.getName());
        cfg.setCategoryPropertyOrder(Configuration.CATEGORY_CLIENT, propOrderGeneral);

        if (readFieldsFromConfig) {
            doUpdateCheck = propDoUpdateCheck.getBoolean(DO_UPDATE_CHECK);
            showBestFriendNotifications = propShowBestFriendNotifications.getBoolean(SHOW_BEST_FRIEND_NOTIFICATIONS);
            showFriendNotifications = propShowFriendNotifications.getBoolean(SHOW_FRIEND_NOTIFICATIONS);
            showGuildNotifications = propShowGuildNotifications.getBoolean(SHOW_GUILD_NOTIFICATIONS);
            moo = propMoo.getString();
        }

        propDoUpdateCheck.set(doUpdateCheck);
        propShowBestFriendNotifications.set(showBestFriendNotifications);
        propShowFriendNotifications.set(showFriendNotifications);
        propShowGuildNotifications.set(showGuildNotifications);
        propMoo.set(moo);

        if (cfg.hasChanged()) {
            cfg.save();
        }
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
