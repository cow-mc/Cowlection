package eu.olli.cowmoonication;

import eu.olli.cowmoonication.command.MooCommand;
import eu.olli.cowmoonication.config.MooConfig;
import eu.olli.cowmoonication.listener.ChatListener;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(modid = Cowmoonication.MODID, version = Cowmoonication.VERSION, clientSideOnly = true, guiFactory = "eu.olli." + Cowmoonication.MODID + ".config.MooGuiFactory")
public class Cowmoonication {
    public static final String MODID = "cowmoonication";
    public static final String VERSION = "1.0";
    private MooConfig config;
    private Friends friends;
    private Utils utils;
    private Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        logger = e.getModLog();

        File modDir = new File(e.getModConfigurationDirectory(), MODID + File.separatorChar);
        if (!modDir.exists()) {
            modDir.mkdirs();
        }

        friends = new Friends(this);
        config = new MooConfig(new Configuration(new File(modDir, MODID + ".cfg")), this);
    }

    @EventHandler
    public void init(FMLInitializationEvent e) {
        utils = new Utils(this);

        MinecraftForge.EVENT_BUS.register(new ChatListener(this));
        ClientCommandHandler.instance.registerCommand(new MooCommand(this));
    }

    public MooConfig getConfig() {
        return config;
    }

    public Friends getFriends() {
        return friends;
    }

    public Utils getUtils() {
        return utils;
    }

    public Logger getLogger() {
        return logger;
    }

}
