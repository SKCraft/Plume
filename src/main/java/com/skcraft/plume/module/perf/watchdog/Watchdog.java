package com.skcraft.plume.module.perf.watchdog;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.skcraft.plume.common.event.lifecycle.InitializationEvent;
import com.skcraft.plume.common.event.lifecycle.LoadConfigEvent;
import com.skcraft.plume.common.event.lifecycle.ShutdownEvent;
import com.skcraft.plume.common.util.Stopwatch;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.event.EventBus;
import com.skcraft.plume.common.util.event.Subscribe;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.event.tick.EntityTickEvent;
import com.skcraft.plume.event.tick.EntityTickExceptionEvent;
import com.skcraft.plume.event.tick.TileEntityTickEvent;
import com.skcraft.plume.event.tick.TileEntityTickExceptionEvent;
import com.skcraft.plume.util.Messages;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import ninja.leaping.configurate.objectmapping.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    // Watchdog thread
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("Plume Watchdog").build());
    private ScheduledFuture<?> future;

    // Updated every tick
    private volatile long lastTick = 0;
    private Thread serverThread;

    // Keep track of the responses to the stall
    private List<Response> stallResponses = Lists.newArrayList();
    private int currentResponseIndex = 0; // Incremented as we iterate through stallResponses, and then reset if server comes back

    // Fields to to make INTERRUPT_TICKING work
    @Getter private final Object threadInterruptLock = new Object();
    @Getter @Setter private Object currentTickingObject; // What we're ticking right now
    @Getter @Setter private boolean catchingTickInterrupt; // Used to determine whether a thrown ThreadDead was one of ours

    @Subscribe
    public void onInitialization(InitializationEvent event) {
        future = scheduledExecutor.scheduleAtFixedRate(new WatchdogTask(), 500, 500, TimeUnit.MILLISECONDS);
    }

    @Subscribe
    public void onLoadConfig(LoadConfigEvent event) {
        List<Response> responses = new ArrayList<>(config.get().stallResponses);
        Collections.sort(responses); // Should be a stable sort
        synchronized (this) {
            stallResponses = responses;
            currentResponseIndex = 0; // This resets our current action if the server is stalled
        }
    }

    @Subscribe
    public void onShutdown(ShutdownEvent event) {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }

    @Subscribe
    public void onTileEntityTick(TileEntityTickEvent event) {
        event.getStopwatches().add(new TickStopwatch(event.getTileEntity()));
    }

    @Subscribe
    public void onEntityTick(EntityTickEvent event) {
        event.getStopwatches().add(new TickStopwatch(event.getEntity()));
    }

    @Subscribe
    public void onTileEntityTickException(TileEntityTickExceptionEvent event) {
        if (catchingTickInterrupt) {
            for (Throwable throwable : Throwables.getCausalChain(event.getThrowable())) {
                if (throwable instanceof ThreadDeath) {
                    TileEntity tileEntity = event.getTileEntity();
                    log.log(Level.WARNING, "The watchdog has raised tick interruption for " + Messages.toString(tileEntity) + " -- removing tile entity!");
                    tileEntity.getWorldObj().setBlock(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, Blocks.air);
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    @Subscribe
    public void onEntityTickException(EntityTickExceptionEvent event) {
        synchronized (threadInterruptLock) { // Lock because we're going to reset catchingTickInterrupt
            if (catchingTickInterrupt) {
                for (Throwable throwable : Throwables.getCausalChain(event.getThrowable())) {
                    if (throwable instanceof ThreadDeath) {
                        Entity entity = event.getEntity();
                        log.log(Level.WARNING, "The watchdog has raised tick interruption for " + Messages.toString(entity) + " -- removing entity!");
                        entity.setDead();
                        event.setCancelled(true);
                        break;
                    }
                }

                catchingTickInterrupt = false;
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == Phase.END) {
            lastTick = System.nanoTime(); // Used to figure out when the server has frozen

            synchronized (Watchdog.this) {
                // If we were stalled before, reset that state
                currentResponseIndex = 0;
            }

            if (serverThread == null) {
                serverThread = Thread.currentThread();
            }
        }
    }

    private class TickStopwatch implements Stopwatch {
        private final Object object;

        private TickStopwatch(Object object) {
            this.object = object;
        }

        @Override
        public void start() {
            synchronized (threadInterruptLock) {
                currentTickingObject = object;
            }
        }

        @Override
        public void stop() {
            synchronized (threadInterruptLock) {
                currentTickingObject = null;
            }
        }
    }

    private class WatchdogTask implements Runnable {
        @Override
        public void run() {
            if (serverThread != null) {
                long now = System.nanoTime();

                synchronized (Watchdog.this) {
                    while (currentResponseIndex < stallResponses.size()) {
                        Response response = stallResponses.get(currentResponseIndex);
                        long stallTime = TimeUnit.NANOSECONDS.toSeconds(now - lastTick);
                        if (stallTime > response.getThreshold()) {
                            try {
                                response.getAction().execute(Watchdog.this, eventBus, serverThread, stallTime);
                            } catch (Throwable e) {
                                log.log(Level.WARNING, "While executing an action for the watchdog, an exception occurred", e);
                            }
                            currentResponseIndex++;
                        } else {
                            break;
                        }
                    }
                }
            }
        }
    }

    private static class WatchdogConfig {
        @Setting(comment = "What to do when the server has stalled, depending on how long it has been stalled for")
        private List<Response> stallResponses = Lists.newArrayList(
                new Response(30, Action.THREAD_DUMP), // 30 seconds
                new Response(600, Action.TERMINATE_SERVER) // 10 minutes
        );
    }

}
