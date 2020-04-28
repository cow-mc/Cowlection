package eu.olli.cowmoonication.listener;

import eu.olli.cowmoonication.Cowmoonication;
import eu.olli.cowmoonication.util.TickDelay;
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
        new TickDelay(() -> main.getChatHelper().sendOfflineMessages(), 6 * 20);
    }

    @SubscribeEvent
    public void onServerLeave(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        main.getFriends().saveBestFriends();
        main.getPlayerCache().clearAllCaches();
    }
}
