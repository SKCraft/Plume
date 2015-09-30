package com.skcraft.plume.common.util.config;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.skcraft.plume.common.event.lifecycle.LoadConfigEvent;
import com.skcraft.plume.common.util.event.Order;
import com.skcraft.plume.common.util.event.Subscribe;
import com.skcraft.plume.common.util.module.AutoRegister;
import lombok.Data;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

@AutoRegister
public class ConfigFactory {

    private final LoadingCache<Key, Config<?>> configs = CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .build(new CacheLoader<Key, Config<?>>() {
                @Override
                public Config<?> load(Key key) throws Exception {
                    File file = new File(dir, key.name + ".cfg");
                    file.getAbsoluteFile().getParentFile().mkdirs();
                    return new Config<>(createLoader(file), key.type);
                }
            });

    private final File dir;

    @Inject
    public ConfigFactory(@DataDir File dir) {
        checkNotNull(dir, "dir");
        this.dir = dir;
    }

    /**]
     * Create a configuration loader.
     *
     * @param file The file
     * @return The loader
     */
    public ConfigurationLoader<CommentedConfigurationNode> createLoader(File file) {
        return HoconConfigurationLoader.builder().setFile(file).build();
    }

    /**]
     * Create a configuration loader using a filename.
     *
     * @param name The filename
     * @return The loader
     */
    public ConfigurationLoader<CommentedConfigurationNode> createLoader(String name) {
        File file = new File(dir, name + ".cfg");
        file.getAbsoluteFile().getParentFile().mkdirs();
        return createLoader(file);
    }

    /**
     * Create a new configuration object whose lifecycle is managed by
     * the factory.
     *
     * @param name The filename or the configuration file
     * @param type The class that will represent the loaded configuration
     * @param <T> The type of the configuration class
     * @return A configuration access object
     */
    @SuppressWarnings("unchecked")
    public <T> Config<T> createMapping(String name, Class<T> type) {
        return (Config<T>) configs.getUnchecked(new Key(name, type));
    }

    @Subscribe(order = Order.VERY_EARLY)
    public void onLoadConfig(LoadConfigEvent event) {
        configs.asMap().values().forEach(Config::load);
    }

    @Subscribe(order = Order.VERY_LATE)
    public void onLoadConfigLate(LoadConfigEvent event) {
        configs.asMap().values().forEach(Config::save);
    }

    @Data
    private static class Key {
        private final String name;
        private final Class<?> type;
    }

}
