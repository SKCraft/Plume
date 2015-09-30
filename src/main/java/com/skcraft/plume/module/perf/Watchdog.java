package com.skcraft.plume.module.perf;

import com.google.common.io.CharSource;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.skcraft.plume.common.event.ReportGenerationEvent;
import com.skcraft.plume.common.event.lifecycle.InitializationEvent;
import com.skcraft.plume.common.event.lifecycle.ShutdownEvent;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.event.EventBus;
import com.skcraft.plume.common.util.event.Subscribe;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.management.ThreadMonitor;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import lombok.extern.java.Log;
import ninja.leaping.configurate.objectmapping.Setting;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Module(name = "watchdog", desc = "Watches the server and detects when it has frozen")
@Log
public class Watchdog {

    @Inject private EventBus eventBus;
    @InjectConfig("watchdog") private Config<WatchdogConfig> config;
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("Plume Watchdog").build());
    private ScheduledFuture<?> future;
    private long lastTick = 0;

    @Subscribe
    public void onInitialization(InitializationEvent event) {
        future = scheduledExecutor.scheduleAtFixedRate(new WatchdogTask(), 500, 500, TimeUnit.MILLISECONDS);
    }

    @Subscribe
    public void onShutdown(ShutdownEvent event) {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == Phase.END) {
            lastTick = System.nanoTime();
        }
    }

    private class WatchdogTask implements Runnable {
        private boolean wasFrozen = false;

        @Override
        public void run() {
            long now = System.nanoTime();

            if (lastTick != 0) {
                if (now - lastTick > TimeUnit.MILLISECONDS.toNanos(config.get().threadDumpThreshold)) {
                    if (!wasFrozen) {
                        log.info("Writing a thread dump because the server has been frozen for some time now");

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ThreadMonitor monitor = new ThreadMonitor();
                        try (PrintStream printStream = new PrintStream(baos)) {
                            monitor.threadDump(printStream);
                        }
                        try {
                            String report = baos.toString("UTF-8");
                            ReportGenerationEvent event = new ReportGenerationEvent("watchdog-freeze", "txt", CharSource.wrap(report));
                            eventBus.post(event);
                        } catch (UnsupportedEncodingException e) {
                            log.log(Level.WARNING, "Failed to write freeze report for Watchdog", e);
                        }
                    }

                    wasFrozen = true;
                } else {
                    wasFrozen = false;
                }
            }
        }
    }

    private static class WatchdogConfig {
        @Setting(comment = "The threshold that the server has been frozen for, in milliseconds, when a thread dump report is created (0 to disable)")
        private long threadDumpThreshold = 30000;
    }

}
