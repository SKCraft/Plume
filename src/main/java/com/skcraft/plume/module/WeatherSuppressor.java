package com.skcraft.plume.module;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.At;
import com.skcraft.plume.command.Group;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldInfo;
import ninja.leaping.configurate.objectmapping.Setting;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Log
@Module(name = "weather-suppressor", desc = "Reduces the incidence of weather")
public class WeatherSuppressor {

    @InjectConfig("weather_suppressor")
    private Config<SuppressorConfig> config;

    private final LoadingCache<Integer, WorldRainState> rainTimeTracker = CacheBuilder.newBuilder()
            .build(new CacheLoader<Integer, WorldRainState>() {
                @Override
                public WorldRainState load(Integer key) throws Exception {
                    return new WorldRainState();
                }
            });

    @Command(aliases = "status", desc = "Reports weather suppressor state information")
    @Group(@At("wsuppress"))
    @Require("plume.weathersuppressor.status")
    public void status(@Sender ICommandSender sender) {
        long now = System.nanoTime();

        if (config.get().suppressRain) {
            int count = 0;

            for (WorldServer world : MinecraftServer.getServer().worldServers) {
                WorldInfo worldInfo = world.getWorldInfo();
                WorldRainState state = rainTimeTracker.getIfPresent(world.provider.dimensionId);
                if (state != null) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("#").append(world.provider.dimensionId).append(" ");
                    builder.append("now=").append(worldInfo.isRaining()).append(" ");
                    builder.append("prev=").append(state.wasRaining).append(" ");
                    builder.append("sinceStart=").append(TimeUnit.NANOSECONDS.toSeconds(now - state.lastRainStartTime)).append("s ");
                    builder.append("sinceEnd=").append(TimeUnit.NANOSECONDS.toSeconds(now - state.lastRainEndTime)).append("s ");
                    sender.addChatMessage(Messages.info(builder.toString()));
                    count++;
                }
            }

            if (count == 0) {
                sender.addChatMessage(Messages.error(tr("weatherSuppressor.noDataAvailable")));
            }
        } else {
            sender.addChatMessage(Messages.error(tr("weatherSuppressor.disabled")));
        }
    }

    @Command(aliases = "reset", desc = "Resets weather suppressor state information")
    @Group(@At("wsuppress"))
    @Require("plume.weathersuppressor.reset")
    public void resetStates(@Sender ICommandSender sender) {
        rainTimeTracker.invalidateAll();
        sender.addChatMessage(Messages.info(tr("weatherSuppressor.stateReset")));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTick(TickEvent.WorldTickEvent event) {
        int dimensionId = event.world.provider.dimensionId;

        if (!config.get().suppressRain || !config.get().affectedDimensions.contains(dimensionId)) {
            return; // Disable switch
        }

        if (event.phase == Phase.START) {
            WorldRainState state = rainTimeTracker.getUnchecked(event.world.provider.dimensionId);
            long now = System.nanoTime();
            WorldInfo worldInfo = event.world.getWorldInfo();

            if (worldInfo.isRaining()) {
                if (state.wasRaining) { // It's supposed to be raining
                    state.lastRainEndTime = now; // Time of last rain = now

                    // But has the rain gone on for too long?
                    long rainElapsedTime = now - state.lastRainStartTime;
                    if (rainElapsedTime >= TimeUnit.MINUTES.toNanos(config.get().rainMaxElapsedTime)) {
                        state.wasRaining = false; // On next tick, rain will be forced to stop

                        if (config.get().logMessages) {
                            log.info("Weather Suppressor: Rain elapsed for too long in dimension " + dimensionId + "! Stopping on next tick...");
                        }
                    }
                } else { // Rain is just starting
                    long timeSinceLastRain = now - state.lastRainEndTime;

                    // Is start allowed?
                    if (timeSinceLastRain >= TimeUnit.MINUTES.toNanos(config.get().delayBetweenRain)) {
                        state.wasRaining = true;
                        state.lastRainStartTime = now;
                        state.lastRainEndTime = now;
                    } else {
                        worldInfo.setRainTime(0);
                        worldInfo.setThunderTime(0);
                        worldInfo.setRaining(false);
                        worldInfo.setThundering(false);

                        if (config.get().logMessages) {
                            log.info("Weather Suppressor: Rain can't start yet in dimension " + dimensionId + "! Time since last rain is " + TimeUnit.NANOSECONDS.toSeconds(timeSinceLastRain) + "s");
                        }
                    }
                }
            }
        }
    }

    public static class SuppressorConfig {
        @Setting(comment = "Whether to suppress rainstorms")
        public boolean suppressRain = false;

        @Setting(comment = "List of dimensions where rain is suppressed (if enabled)")
        public Set<Integer> affectedDimensions = Sets.newHashSet(0);

        @Setting(comment = "Write to the server log when this module suppresses rainstorms")
        public boolean logMessages = false;

        @Setting(comment = "The number of real life minutes that must pass before a rainstorm can start again")
        public long delayBetweenRain = 30;

        @Setting(comment = "The maximum number of real life minutes that a rainstorm may last for")
        public long rainMaxElapsedTime = 5;
    }

    private static class WorldRainState {
        private boolean wasRaining = true;
        private long lastRainStartTime = System.nanoTime();
        private long lastRainEndTime = System.nanoTime();
    }

}
