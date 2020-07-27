package de.cowtipper.cowlection;

import de.cowtipper.cowlection.command.MooCommand;
import de.cowtipper.cowlection.command.ReplyCommand;
import de.cowtipper.cowlection.command.ShrugCommand;
import de.cowtipper.cowlection.command.TabCompletableCommand;
import de.cowtipper.cowlection.config.MooConfig;
import de.cowtipper.cowlection.handler.DungeonCache;
import de.cowtipper.cowlection.handler.FriendsHandler;
import de.cowtipper.cowlection.handler.PlayerCache;
import de.cowtipper.cowlection.listener.ChatListener;
import de.cowtipper.cowlection.listener.PlayerListener;
import de.cowtipper.cowlection.util.ChatHelper;
import de.cowtipper.cowlection.util.VersionChecker;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(modid = Cowlection.MODID, name = Cowlection.MODNAME, version = Cowlection.VERSION,
        clientSideOnly = true,
        guiFactory = "@PACKAGE@.config.MooGuiFactory",
        updateJSON = "https://raw.githubusercontent.com/cow-mc/Cowlection/master/update.json")
public class Cowlection {
    public static final String MODID = "@MODID@";
    public static final String VERSION = "@VERSION@";
    public static final String MODNAME = "@MODNAME@";
    public static final String GITURL = "@GITURL@";
    private static Cowlection instance;
    private File configDir;
    private File modsDir;
    private MooConfig config;
    private FriendsHandler friendsHandler;
    private VersionChecker versionChecker;
    private ChatHelper chatHelper;
    private PlayerCache playerCache;
    private DungeonCache dungeonCache;
    private Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        instance = this;
        logger = e.getModLog();
        modsDir = e.getSourceFile().getParentFile();

        this.configDir = new File(e.getModConfigurationDirectory(), MODID + File.separatorChar);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        friendsHandler = new FriendsHandler(this, new File(configDir, "friends.json"));
        config = new MooConfig(this, new Configuration(new File(configDir, MODID + ".cfg")));

        chatHelper = new ChatHelper();
    }

    @EventHandler
    public void init(FMLInitializationEvent e) {
        MinecraftForge.EVENT_BUS.register(new ChatListener(this));
        MinecraftForge.EVENT_BUS.register(new PlayerListener(this));
        ClientCommandHandler.instance.registerCommand(new MooCommand(this));
        ClientCommandHandler.instance.registerCommand(new ReplyCommand(this));
        ClientCommandHandler.instance.registerCommand(new ShrugCommand(this));
        for (String tabCompletableNamesCommand : MooConfig.tabCompletableNamesCommands) {
            ClientCommandHandler.instance.registerCommand(new TabCompletableCommand(this, tabCompletableNamesCommand));
        }
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent e) {
        versionChecker = new VersionChecker(this);
        playerCache = new PlayerCache(this);
    }

    public MooConfig getConfig() {
        return config;
    }

    public FriendsHandler getFriendsHandler() {
        return friendsHandler;
    }

    public VersionChecker getVersionChecker() {
        return versionChecker;
    }

    public ChatHelper getChatHelper() {
        return chatHelper;
    }

    public PlayerCache getPlayerCache() {
        return playerCache;
    }

    public DungeonCache getDungeonCache() {
        if (dungeonCache == null) {
            dungeonCache = new DungeonCache(this);
        }
        return dungeonCache;
    }

    public File getConfigDirectory() {
        return configDir;
    }

    public File getModsDirectory() {
        return modsDir;
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * Get mod's instance; instead of this method use dependency injection where possible
     */
    public static Cowlection getInstance() {
        return instance;
    }
}
