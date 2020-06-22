package eu.olli.cowmoonication;

import eu.olli.cowmoonication.command.MooCommand;
import eu.olli.cowmoonication.command.ShrugCommand;
import eu.olli.cowmoonication.command.TabCompletableCommand;
import eu.olli.cowmoonication.config.MooConfig;
import eu.olli.cowmoonication.handler.FriendsHandler;
import eu.olli.cowmoonication.handler.PlayerCache;
import eu.olli.cowmoonication.listener.ChatListener;
import eu.olli.cowmoonication.listener.PlayerListener;
import eu.olli.cowmoonication.util.ChatHelper;
import eu.olli.cowmoonication.util.VersionChecker;
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

@Mod(modid = Cowmoonication.MODID, name = Cowmoonication.MODNAME, version = Cowmoonication.VERSION,
        clientSideOnly = true,
        guiFactory = "@PACKAGE@.config.MooGuiFactory",
        updateJSON = "https://raw.githubusercontent.com/cow-mc/Cowmoonication/master/update.json")
public class Cowmoonication {
    public static final String MODID = "@MODID@";
    public static final String VERSION = "@VERSION@";
    public static final String MODNAME = "@MODNAME@";
    public static final String GITURL = "@GITURL@";
    private static Cowmoonication instance;
    private File configDir;
    private File modsDir;
    private MooConfig config;
    private FriendsHandler friendsHandler;
    private VersionChecker versionChecker;
    private ChatHelper chatHelper;
    private PlayerCache playerCache;
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

    /**
     * Get mod's instance; instead of this method use dependency injection where possible
     */
    public static Cowmoonication getInstance() {
        return instance;
    }
}
