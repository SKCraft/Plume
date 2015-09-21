package com.skcraft.plume.module.perf.profiler;

import lombok.Getter;
import lombok.Setter;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

class PendingProfile extends TimerTask {

    @Getter private final Profiler profiler = new Profiler();
    @Getter private final Timer timer = new Timer();
    @Getter @Setter private boolean collecting = true;
    private final Consumer<Profiler> completionConsumer;

    public PendingProfile(int delay, Consumer<Profiler> completionConsumer) {
        this.completionConsumer = completionConsumer;
        timer.schedule(this, delay);
    }

    public void stop() {
        collecting = false;
        timer.cancel();
        completionConsumer.accept(profiler);
    }

    @Override
    public void run() {
        stop();
    }

}
