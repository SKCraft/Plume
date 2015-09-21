package com.skcraft.plume.module.perf;

import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.skcraft.plume.common.event.lifecycle.LoadConfigEvent;
import com.skcraft.plume.common.util.XorRandom;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.event.tick.EntityTickEvent;
import com.skcraft.plume.event.tick.TileEntityTickEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@Module(name = "dictator", desc = "Reduce the ticking frequency of certain entities or tile entities", enabled = false)
public class Dictator {

    private static final XorRandom random = new XorRandom();
    private static final LoadingCache<String, Class<?>> CLASS_CACHE = CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .weakValues()
            .build(new CacheLoader<String, Class<?>>() {
                @Override
                public Class<?> load(@Nullable String key) throws Exception {
                    try {
                        return Class.forName(key);
                    } catch (ClassNotFoundException e) {
                        return void.class;
                    }
                }
            });

    @InjectConfig("dictator")
    private Config<DictatorConfig> config;
    private final LoadingCache<Class<?>, Optional<Rule>> ruleCache = CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .weakKeys()
            .build(new CacheLoader<Class<?>, Optional<Rule>>() {
                @Override
                public Optional<Rule> load(@Nullable Class<?> key) throws Exception {
                    return Optional.ofNullable(getRule(key));
                }
            });

    private Rule getRule(Class<?> clazz) {
        for (Rule rule : config.get().rules) {
            if (rule.apply(clazz)) {
                return rule;
            }
        }
        return null;
    }

    @Subscribe
    public void onLoadConfig(LoadConfigEvent event) {
        ruleCache.invalidateAll();
    }

    @SubscribeEvent
    public void onEntityTickEvent(EntityTickEvent event) {
        if (!config.get().rules.isEmpty()) {
            Optional<Rule> optional = ruleCache.getUnchecked(event.getEntity().getClass());
            if (optional.isPresent()) {
                event.setCanceled(!optional.get().mayTick());
            }
        }
    }

    @SubscribeEvent
    public void onTileEntityTickEvent(TileEntityTickEvent event) {
        if (!config.get().rules.isEmpty()) {
            Optional<Rule> optional = ruleCache.getUnchecked(event.getTileEntity().getClass());
            if (optional.isPresent()) {
                event.setCanceled(!optional.get().mayTick());
            }
        }
    }

    private static class DictatorConfig {
        @Setting(comment = "List of rules to reduce ticking with")
        private List<Rule> rules = Lists.newArrayList();
    }

    @ConfigSerializable
    private static class Rule implements Predicate<Class<?>> {
        @Setting(comment = "Class names to match")
        private List<String> instanceOf = Lists.newArrayList();

        @Setting(comment = "Class names to not match")
        private List<String> notInstanceOf = Lists.newArrayList();

        @Setting(comment = "The chance (0 to 1) of ticking, where 1 is 100% or every time (<= 0 to not tick at all)")
        private double chance = 1;

        @Override
        public boolean apply(Class<?> input) {
            if (instanceOf == null) return false;

            boolean matches = false;

            for (String className : instanceOf) {
                Class<?> clazz = CLASS_CACHE.getUnchecked(className);
                if (clazz != void.class) {
                    if (clazz.isAssignableFrom(input)) {
                        matches = true;
                    } else {
                        return false;
                    }
                }
            }

            if (!matches) {
                return false;
            }

            if (notInstanceOf != null) {
                for (String className : notInstanceOf) {
                    Class<?> clazz = CLASS_CACHE.getUnchecked(className);
                    if (clazz != void.class) {
                        if (clazz.isAssignableFrom(input)) {
                            return false;
                        }
                    }
                }
            }

            return true;
        }

        public boolean mayTick() {
            if (chance >= 1) {
                return true;
            }
            double p = Math.abs(random.nextLong() / (double) Long.MAX_VALUE);
            return chance > 0 && p <= chance;
        }
    }


}
