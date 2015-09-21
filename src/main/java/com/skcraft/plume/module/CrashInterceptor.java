package com.skcraft.plume.module;

import com.google.common.base.Supplier;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.skcraft.plume.common.event.lifecycle.LoadConfigEvent;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.event.tick.EntityTickEvent;
import com.skcraft.plume.event.tick.EntityTickExceptionEvent;
import com.skcraft.plume.event.tick.TileEntityTickEvent;
import com.skcraft.plume.event.tick.TileEntityTickExceptionEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import lombok.extern.java.Log;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import ninja.leaping.configurate.objectmapping.Setting;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Module(name = "crash-interceptor", desc = "Intercepts certain kinds of crashes")
@Log
public class CrashInterceptor {

    @InjectConfig("crash_interceptor")
    private Config<CrashConfig> config;
    private Cache<Object, Boolean> noTickCache;
    private final LoadingCache<Object, CrashTracker> logCache = CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .weakKeys()
            .build(new CacheLoader<Object, CrashTracker>() {
                @Override
                public CrashTracker load(Object key) throws Exception {
                    return new CrashTracker();
                }
            });

    @Subscribe
    public void onLoadConfig(LoadConfigEvent event) {
        noTickCache = CacheBuilder.newBuilder()
                .concurrencyLevel(1)
                .weakKeys()
                .expireAfterAccess(config.get().disableTime, TimeUnit.MILLISECONDS)
                .build();
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent event) {
        if (noTickCache.asMap().containsKey(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onEntityTickException(EntityTickExceptionEvent event) {
        if (config.get().crashResponse == CrashResponse.SUPPRESS_AND_LOG) {
            noTickCache.put(event.getEntity(), true);
            logCache.getUnchecked(event.getEntity()).log(event.getThrowable(), () -> getEntityString(event.getEntity()));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onTileEntityTick(TileEntityTickEvent event) {
        if (noTickCache.asMap().containsKey(event.getTileEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onTileEntityTickException(TileEntityTickExceptionEvent event) {
        if (config.get().crashResponse == CrashResponse.SUPPRESS_AND_LOG) {
            noTickCache.put(event.getTileEntity(), true);
            logCache.getUnchecked(event.getTileEntity()).log(event.getThrowable(), () -> getTileEntityString(event.getTileEntity()));
            event.setCanceled(true);
        }
    }

    private static String getEntityString(Entity entity) {
        return "Entity: {dim=" + entity.worldObj.provider.dimensionId +
                " pos=" + entity.posX + "," + entity.posY + "," + entity.posZ +
                " (" + entity.getClass().getName() + ")}";
    }

    private static String getTileEntityString(TileEntity tileEntity) {
        return "TileEntity: {dim=" + tileEntity.getWorldObj().provider.dimensionId +
                " pos=" + tileEntity.xCoord + "," + tileEntity.yCoord + "," + tileEntity.zCoord +
                " (" + tileEntity.getClass().getName() + ")}";
    }

    private class CrashTracker {
        private long lastMessage;

        public void log(Throwable throwable, Supplier<String> infoString) {
            long now = System.nanoTime();
            if (now - lastMessage > TimeUnit.MILLISECONDS.toNanos(config.get().logInterval)) {
                lastMessage = now;
                log.log(Level.WARNING, "Crash intercepted for " + infoString.get(), throwable);
            }
        }
    }

    private static class CrashConfig {
        @Setting(comment = "The action to take when a crash occurs")
        public CrashResponse crashResponse = CrashResponse.DEFAULT;

        @Setting(comment = "How long to disable a block or entity for if it crashes, in milliseconds")
        public long disableTime = TimeUnit.SECONDS.toMillis(10);

        @Setting(comment = "How long to wait before logging a crash again for a crashing block or entity, in milliseconds")
        public long logInterval = TimeUnit.SECONDS.toMillis(30);
    }

    private enum CrashResponse {
        DEFAULT,
        SUPPRESS_AND_LOG
    }

}
