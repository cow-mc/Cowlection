package de.cowtipper.cowlection.util;

public abstract class AbortableRunnable implements Runnable {
    protected boolean stopped = false;

    public abstract void run();

    public void stop() {
        stopped = true;
    }
}
