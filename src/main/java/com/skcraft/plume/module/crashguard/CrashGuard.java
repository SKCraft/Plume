package com.skcraft.plume.module.crashguard;

import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.event.Subscribe;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.event.CrashEvent;
import com.skcraft.plume.event.tick.EntityTickExceptionEvent;
import com.skcraft.plume.event.tick.TileEntityTickExceptionEvent;
import lombok.extern.java.Log;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Module(name = "crash-guard", desc = "Intercepts certain kinds of crashes")
@Log
public class CrashGuard {

    @InjectConfig("crash_guard")
    private Config<CrashGuardConfig> config;
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
    public void onCrash(CrashEvent event) {
        CrashHandler crashHandler;

        if (event.getThrowable() instanceof ThreadDeath) {
            return; // Used by Watchdog
        } else if (event.getThrowable() instanceof StackOverflowError) {
            crashHandler = config.get().crashEvents.onStackOverflow;
        } else if (event.getThrowable() instanceof LinkageError) {
            crashHandler = config.get().crashEvents.onLinkageError;
        } else if (event.getThrowable() instanceof Exception) {
            crashHandler = config.get().crashEvents.onException;
        } else {
            crashHandler = config.get().crashEvents.onOtherError;
        }

        boolean result;

        if (event instanceof EntityTickExceptionEvent) {
            result = crashHandler.apply(this, event.getThrowable(), ((EntityTickExceptionEvent) event).getEntity());
        } else if (event instanceof TileEntityTickExceptionEvent) {
            result = crashHandler.apply(this, event.getThrowable(), ((TileEntityTickExceptionEvent) event).getTileEntity());
        } else {
            result = crashHandler.apply(this, event.getThrowable());
        }

        event.setCancelled(result);
    }

    public void logCrashPeriodically(@Nullable Object key, Throwable t, Supplier<String> infoString) {
        if (key != null) {
            logCache.getUnchecked(key).log(t, infoString);
        } else {
            log.log(Level.WARNING, "Crash intercepted", t);
        }
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

    private static class CrashGuardConfig {
        @Setting(comment = "Choose, based on the type of error, what to do; choices: DEFAULT (crashes server/uses defaults), SUPPRESS (suppress the crash and log it), and REMOVE (try to remove the entity/tile entity and suppress the crash)")
        public HandlerSettings crashEvents = new HandlerSettings();

        @Setting(comment = "How long to wait before logging a crash again for a crashing block or entity, in milliseconds")
        public long logInterval = TimeUnit.SECONDS.toMillis(30);
    }

    @ConfigSerializable
    private static class HandlerSettings {
        @Setting(comment = "The action to take when a 'normal' crash occurs (covers most crashes)")
        public CrashHandler onException = CrashHandler.DEFAULT;

        @Setting(comment = "The action to take when a 'stack overflow' occurs (usually a programming error)")
        public CrashHandler onStackOverflow = CrashHandler.DEFAULT;

        @Setting(comment = "The action to take when a severe 'linkage error' occurs (Java class file is incompatible or corrupt)")
        public CrashHandler onLinkageError = CrashHandler.DEFAULT;

        @Setting(comment = "The action to take for all other types of errors (such as out of memory and other very severe errors)")
        public CrashHandler onOtherError = CrashHandler.DEFAULT;
    }

}
