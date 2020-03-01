package eu.olli.cowmoonication.config;

import eu.olli.cowmoonication.Cowmoonication;
import eu.olli.cowmoonication.Utils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class MooConfig {
    public static boolean filterFriendNotifications;
    private static String[] bestFriends;
    private static Configuration cfg = null;
    private final Cowmoonication main;

    public MooConfig(Configuration configuration, Cowmoonication main) {
        this.main = main;
        cfg = configuration;
        initConfig();
    }

    public static Configuration getConfig() {
        return cfg;
    }

    private void initConfig() {
        syncFromFile();
        main.getFriends().syncFriends(bestFriends);
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
    private void syncFromFields() {
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

        final boolean FILTER_FRIEND_NOTIFICATIONS = true;
        Property propFilterFriendNotify = cfg.get(Configuration.CATEGORY_CLIENT, "filterFriendNotifications", FILTER_FRIEND_NOTIFICATIONS, "Set to false to receive all login/logout messages, set to true to only get notifications of 'best friends' joining/leaving");

        final String[] BEST_FRIENDS_DEFAULT_VALUE = new String[]{"Cow"};
        Property propBestFriends = cfg.get(Configuration.CATEGORY_CLIENT, "bestFriends", BEST_FRIENDS_DEFAULT_VALUE, "List of best friends: receive login/logout notifications from them");
        propBestFriends.setValidationPattern(Utils.VALID_USERNAME);

        List<String> propOrderGeneral = new ArrayList<>();
        propOrderGeneral.add(propFilterFriendNotify.getName());
        propOrderGeneral.add(propBestFriends.getName());
        cfg.setCategoryPropertyOrder(Configuration.CATEGORY_CLIENT, propOrderGeneral);

        if (readFieldsFromConfig) {
            filterFriendNotifications = propFilterFriendNotify.getBoolean(FILTER_FRIEND_NOTIFICATIONS);
            bestFriends = propBestFriends.getStringList();
        }

        propFilterFriendNotify.set(filterFriendNotifications);
        propBestFriends.set(bestFriends);

        if (cfg.hasChanged()) {
            cfg.save();
        }
        if (propBestFriends.hasChanged()) {
            main.getFriends().syncFriends(bestFriends);
        }
    }

    public void toggleNotifications() {
        filterFriendNotifications = !filterFriendNotifications;
        syncFromFields();
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
