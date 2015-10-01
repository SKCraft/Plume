package com.skcraft.plume.module.perf.watchdog;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.skcraft.plume.common.event.lifecycle.InitializationEvent;
import com.skcraft.plume.common.event.lifecycle.LoadConfigEvent;
import com.skcraft.plume.common.event.lifecycle.ShutdownEvent;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.event.EventBus;
import com.skcraft.plume.common.util.event.Subscribe;
import com.skcraft.plume.common.util.module.Module;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import lombok.extern.java.Log;
import ninja.leaping.configurate.objectmapping.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Module(name = "watchdog", desc = "Watches the server and detects when it has frozen")
@Log
public class Watchdog {

    @Inject private EventBus eventBus;
    @InjectConfig("watchdog") private Config<WatchdogConfig> config;
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("Plume Watchdog").build());
    private ScheduledFuture<?> future;
    private volatile long lastTick = 0;
    private List<Response> stallResponses = Lists.newArrayList();
    private int currentResponseIndex = 0;
    private Thread serverThread;

    @Subscribe
    public void onInitialization(InitializationEvent event) {
        future = scheduledExecutor.scheduleAtFixedRate(new WatchdogTask(), 500, 500, TimeUnit.MILLISECONDS);
    }

    @Subscribe
    public void onLoadConfig(LoadConfigEvent event) {
        List<Response> responses = new ArrayList<>(config.get().stallResponses);
        Collections.sort(responses);
        synchronized (this) {
            stallResponses = responses;
            currentResponseIndex = 0;
        }
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
            synchronized (Watchdog.this) {
                currentResponseIndex = 0;
            }

            if (serverThread == null) {
                serverThread = Thread.currentThread();
            }
        }
    }

    private class WatchdogTask implements Runnable {
        @Override
        public void run() {
            if (serverThread != null) {
                long now = System.nanoTime();

                synchronized (Watchdog.this) {
                    if (currentResponseIndex < stallResponses.size()) {
                        Response response = stallResponses.get(currentResponseIndex);
                        if (now - lastTick > TimeUnit.SECONDS.toNanos(response.getTime())) {
                            response.getAction().execute(eventBus, serverThread);
                            currentResponseIndex++;
                        }
                    }
                }
            }
        }
    }

    private static class WatchdogConfig {
        @Setting(comment = "What to do when the server has stalled, depending on how long it has been stalled for")
        private List<Response> stallResponses = Lists.newArrayList(
                new Response(30, Action.THREAD_DUMP),
                new Response(600, Action.GRACEFUL_SHUTDOWN),
                new Response(720, Action.TERMINATE_SERVER)
        );
    }

}
