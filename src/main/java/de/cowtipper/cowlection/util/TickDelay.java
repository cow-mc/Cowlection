package de.cowtipper.cowlection.util;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class TickDelay {
    private Runnable task;
    private int waitingTicks;

    public TickDelay(Runnable task, int ticks) {
        this.task = task;
        this.waitingTicks = ticks;

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.START) {
            if (waitingTicks < 1) {
                // we're done waiting! Do stuff and exit.
                task.run();
                MinecraftForge.EVENT_BUS.unregister(this);
            }
            waitingTicks--;
        }
    }
}
