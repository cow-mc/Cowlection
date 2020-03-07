package eu.olli.cowmoonication.listener;

import eu.olli.cowmoonication.Cowmoonication;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

public class PlayerListener {
    private final Cowmoonication main;

    public PlayerListener(Cowmoonication main) {
        this.main = main;
    }

    @SubscribeEvent
    public void onServerJoin(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        main.getVersionChecker().runUpdateCheck(false);
    }
}
