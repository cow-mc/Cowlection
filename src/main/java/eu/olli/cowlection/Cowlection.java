package eu.olli.cowlection;

import eu.olli.cowlection.command.MooCommand;
import eu.olli.cowlection.command.ShrugCommand;
import eu.olli.cowlection.command.TabCompletableCommand;
import eu.olli.cowlection.config.MooConfig;
import eu.olli.cowlection.handler.FriendsHandler;
import eu.olli.cowlection.handler.PlayerCache;
import eu.olli.cowlection.listener.ChatListener;
import eu.olli.cowlection.listener.PlayerListener;
import eu.olli.cowlection.util.ChatHelper;
import eu.olli.cowlection.util.VersionChecker;
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
    private boolean isOnSkyBlock;
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

    public File getConfigDirectory() {
        return configDir;
    }

    public File getModsDirectory() {
        return modsDir;
    }

    public Logger getLogger() {
        return logger;
    }

    public boolean isOnSkyBlock() {
        return isOnSkyBlock;
    }

    public void setIsOnSkyBlock(boolean isOnSkyBlock) {
        this.isOnSkyBlock = isOnSkyBlock;
    }

    /**
     * Get mod's instance; instead of this method use dependency injection where possible
     */
    public static Cowlection getInstance() {
        return instance;
    }
}
