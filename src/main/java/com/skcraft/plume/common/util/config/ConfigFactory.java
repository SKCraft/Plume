package com.skcraft.plume.common.util.config;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.util.eventbus.EventHandler.Priority;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.skcraft.plume.common.event.lifecycle.LoadConfigEvent;
import com.skcraft.plume.common.util.module.AutoRegister;
import lombok.Data;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
@AutoRegister
public class ConfigFactory {

    private final LoadingCache<Key, Config<?>> configs = CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .build(new CacheLoader<Key, Config<?>>() {
                @Override
                public Config<?> load(Key key) throws Exception {
                    File file = new File(dir, key.name + ".cfg");
                    file.getAbsoluteFile().getParentFile().mkdirs();
                    return new HoconConfig<>(file, key.type);
                }
            });

    private final File dir;

    @Inject
    public ConfigFactory(@DataDir File dir) {
        checkNotNull(dir, "dir");
        this.dir = dir;
    }

    @SuppressWarnings("unchecked")
    public <T> Config<T> create(String name, Class<T> type) {
        return (Config<T>) configs.getUnchecked(new Key(name, type));
    }

    @Subscribe(priority = Priority.VERY_EARLY)
    public void onLoadConfig(LoadConfigEvent event) {
        configs.asMap().values().forEach(Config::load);
    }

    @Subscribe(priority = Priority.VERY_LATE)
    public void onLoadConfigLate(LoadConfigEvent event) {
        configs.asMap().values().forEach(Config::save);
    }

    @Data
    private static class Key {
        private final String name;
        private final Class<?> type;
    }

}
