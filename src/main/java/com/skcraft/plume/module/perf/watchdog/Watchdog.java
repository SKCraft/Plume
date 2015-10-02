package com.skcraft.plume.module.perf.watchdog;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.skcraft.plume.common.event.lifecycle.InitializationEvent;
import com.skcraft.plume.common.event.lifecycle.LoadConfigEvent;
import com.skcraft.plume.common.event.lifecycle.ShutdownEvent;
import com.skcraft.plume.common.util.Stopwatch;
import com.skcraft.plume.common.util.StringInterpolation;
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
import net.minecraft.util.ChatComponentText;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "watchdog", desc = "Watches the server and detects when it has frozen")
@Log
public class Watchdog {

    @Inject private EventBus eventBus;
    @InjectConfig("watchdog") private Config<WatchdogConfig> config;
    @Inject private StallCauseDetector causeDetector;

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
                    Thread.interrupted(); // Clear interrupted flag
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
                        Thread.interrupted(); // Clear interrupted flag
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
        private boolean wasStalled = false;
        private long lastStallStartTime = 0;
        private long lastMessageTime = 0;

        private String getStallMessage(String message, long time) {
            return StringInterpolation.interpolate(StringInterpolation.BRACE_PATTERN, message, input -> {
                switch (input) {
                    case "time":
                        return String.valueOf(time);
                    case "cause":
                        String cause = causeDetector.analyze(serverThread);
                        if (cause != null) {
                            return cause;
                        } else {
                            return tr("watchdog.cause.unknown");
                        }
                    default:
                        return null;
                }
            });
        }

        @Override
        public void run() {
            if (serverThread != null) {
                long now = System.nanoTime();
                long stallTime = TimeUnit.NANOSECONDS.toSeconds(now - lastTick);

                if (stallTime >= 1) {
                    if (!wasStalled) {
                        lastStallStartTime = System.nanoTime();
                        lastMessageTime = System.nanoTime();
                        wasStalled = true;
                    }

                    if (config.get().broadcastDuringStall.enabled && TimeUnit.NANOSECONDS.toSeconds(now - lastMessageTime) >= config.get().broadcastDuringStall.interval) {
                        Messages.broadcast(new ChatComponentText(getStallMessage(config.get().broadcastDuringStall.message, stallTime)));
                        lastMessageTime = System.nanoTime();
                    }
                } else {
                    if (wasStalled) {
                        long lastStallTime = TimeUnit.NANOSECONDS.toSeconds(now - lastStallStartTime);

                        if (config.get().broadcastOnStallEnd.enabled && lastStallTime >= config.get().broadcastOnStallEnd.threshold) {
                            Messages.broadcast(new ChatComponentText(getStallMessage(config.get().broadcastOnStallEnd.message, lastStallTime)));
                        }

                        wasStalled = false;
                    }
                }

                synchronized (Watchdog.this) {
                    while (currentResponseIndex < stallResponses.size()) {
                        Response response = stallResponses.get(currentResponseIndex);
                        if (stallTime > response.getThreshold()) {
                            try {
                                response.getAction().execute(response, Watchdog.this, eventBus, serverThread, stallTime);
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
        @Setting(comment = "Options to adjust a message that can be printed periodically during an ongoing server stall")
        private StallIntervalMessage broadcastDuringStall = new StallIntervalMessage();

        @Setting(comment = "The message broadcast when a server stall ends")
        private StallEndMessage broadcastOnStallEnd = new StallEndMessage();

        @Setting(comment = "What to do when the server has stalled, depending on how long it has been stalled for")
        private List<Response> stallResponses = Lists.newArrayList(
                new Response(30, Action.THREAD_DUMP),
                new Response(590, Action.BROADCAST, "\u00a4The server will be forcefully shutdown shortly."),
                new Response(600, Action.GRACEFUL_SHUTDOWN), // 10 minutes
                new Response(610, Action.TERMINATE_SERVER)
        );
    }

    @ConfigSerializable
    private static class StallIntervalMessage {
        private boolean enabled = true;

        @Setting(comment = "The interval (in seconds) between broadcast messages")
        private long interval = 5;

        @Setting(comment = "The message, with {time} and {cause} as variables")
        private String message = "\u00a74The server has stalled for {time} seconds (cause: {cause}).";
    }

    @ConfigSerializable
    private static class StallEndMessage {
        private boolean enabled = true;

        @Setting(comment = "The threshold (in seconds) that a stall must persist for")
        private long threshold = 5;

        @Setting(comment = "The message, with {time} as a variable")
        private String message = "\u00a72The server has resumed.";
    }

}
