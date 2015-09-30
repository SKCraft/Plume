package com.skcraft.plume.module.perf;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.skcraft.plume.common.event.lifecycle.LoadConfigEvent;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.event.Subscribe;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.event.world.ChunkLoadRequestEvent;
import com.skcraft.plume.util.config.ClassPattern;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.tileentity.TileEntity;
import ninja.leaping.configurate.objectmapping.Setting;

import javax.annotation.Nullable;
import java.util.List;

@Module(name = "chunk-load-limiter", desc = "Limits implicit chunk loading depending on a configuration")
public class ChunkLoadLimiter {

    @InjectConfig("chunk_load_limiter") private Config<LimiterConfig> config;
    @Inject private CurrentTickingObject currentTickingObject;
    private boolean tickingWorld = false;

    private final LoadingCache<Class<?>, Boolean> ruleCache = CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .weakKeys()
            .build(new CacheLoader<Class<?>, Boolean>() {
                @Override
                public Boolean load(@Nullable Class<?> key) throws Exception {
                    return mayLoadChunks(key);
                }
            });

    private boolean mayLoadChunks(Class<?> clazz) {
        List<ClassPattern> whitelist = config.get().whitelist;
        List<ClassPattern> blacklist = config.get().blacklist;

        if (blacklist != null && !blacklist.isEmpty()) {
            for (ClassPattern pattern : blacklist) {
                if (pattern.apply(clazz)) {
                    return false;
                }
            }
        }

        if (whitelist != null && !whitelist.isEmpty()) {
            for (ClassPattern pattern : whitelist) {
                if (pattern.apply(clazz)) {
                    return true;
                }
            }

            return false;
        } else{
            return true;
        }
    }

    @Subscribe
    public void onLoadConfig(LoadConfigEvent event) {
        ruleCache.invalidateAll();
    }

    @SubscribeEvent
    public void onChunkLoadRequest(ChunkLoadRequestEvent event) {
        List<ClassPattern> whitelist = config.get().whitelist;
        List<ClassPattern> blacklist = config.get().blacklist;

        TileEntity tileEntity = currentTickingObject.getCurrentTileEntity();
        if (tileEntity != null) {
            if (whitelist != null && !whitelist.isEmpty() || blacklist != null && !blacklist.isEmpty()) {
                event.setCanceled(!ruleCache.getUnchecked(tileEntity.getClass()));
            }
        } else if (tickingWorld) {
            if (!config.get().allowLoadingDuringWorldTick) {
                event.setCanceled(true);
            }
        } else {
            if (!config.get().allowLoadingElsewhere) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onWorldTickStart(TickEvent.WorldTickEvent event) {
        tickingWorld = true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onWorldTickEnd(TickEvent.WorldTickEvent event) {
        tickingWorld = false;
    }

    private static class LimiterConfig {
        @Setting(comment = "If non-empty, restricts implicit chunk loading to the matching tile entities")
        private List<ClassPattern> whitelist = Lists.newArrayList();

        @Setting(comment = "List of tile entities that can't implicit chunk load")
        private List<ClassPattern> blacklist = Lists.newArrayList();

        @Setting(comment = "Whether to allow implicit chunk loading during the Forge 'world tick' event")
        private boolean allowLoadingDuringWorldTick = true;

        @Setting(comment = "Whether to allow implicit chunk loading at other times (other than world tick, tile entity ticking)")
        private boolean allowLoadingElsewhere = true;
    }

}
