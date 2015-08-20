package com.skcraft.plume.common.util.config;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.util.eventbus.EventHandler.Priority;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.skcraft.plume.common.event.lifecycle.LoadConfigEvent;
import com.skcraft.plume.common.util.module.AutoRegister;

import java.io.File;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
@AutoRegister
public class ConfigFactory {

    private final List<Config<?>> configs = Lists.newArrayList();

    private final File dir;

    @Inject
    public ConfigFactory(@DataDir File dir) {
        checkNotNull(dir, "dir");
        this.dir = dir;
    }

    public <T> Config<T> create(String name, Class<T> type) {
        File file = new File(dir, name + ".cfg");
        file.getAbsoluteFile().getParentFile().mkdirs();
        Config<T> config = new HoconConfig<T>(file, type);
        configs.add(config);
        return config;
    }

    @Subscribe(priority = Priority.VERY_EARLY)
    public void onLoadConfig(LoadConfigEvent event) {
        configs.forEach(Config::load);
    }

    @Subscribe(priority = Priority.VERY_LATE)
    public void onLoadConfigLate(LoadConfigEvent event) {
        configs.forEach(Config::save);
    }

}
