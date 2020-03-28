package eu.olli.cowmoonication;

import eu.olli.cowmoonication.command.MooCommand;
import eu.olli.cowmoonication.config.MooConfig;
import eu.olli.cowmoonication.friends.Friends;
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
        guiFactory = "eu.olli." + Cowmoonication.MODID + ".config.MooGuiFactory",
        updateJSON = "https://raw.githubusercontent.com/cow-mc/Cowmoonication/master/update.json")
public class Cowmoonication {
    public static final String MODID = "cowmoonication";
    public static final String VERSION = "1.8.9-0.2.0";
    public static final String MODNAME = "Cowmoonication";
    private File modsDir;
    private MooConfig config;
    private Friends friends;
    private VersionChecker versionChecker;
    private ChatHelper chatHelper;
    private Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        logger = e.getModLog();

        File modDir = new File(e.getModConfigurationDirectory(), MODID + File.separatorChar);
        if (!modDir.exists()) {
            modDir.mkdirs();
        }

        friends = new Friends(this, new File(modDir, "friends.json"));
        config = new MooConfig(new Configuration(new File(modDir, MODID + ".cfg")));

        chatHelper = new ChatHelper();
        modsDir = e.getSourceFile().getParentFile();
    }

    @EventHandler
    public void init(FMLInitializationEvent e) {

        MinecraftForge.EVENT_BUS.register(new ChatListener(this));
        MinecraftForge.EVENT_BUS.register(new PlayerListener(this));
        ClientCommandHandler.instance.registerCommand(new MooCommand(this));
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent e) {
        versionChecker = new VersionChecker(this);
    }

    public MooConfig getConfig() {
        return config;
    }

    public Friends getFriends() {
        return friends;
    }

    public VersionChecker getVersionChecker() {
        return versionChecker;
    }

    public ChatHelper getChatHelper() {
        return chatHelper;
    }

    public File getModsFolder() {
        return modsDir;
    }

    public Logger getLogger() {
        return logger;
    }
}
