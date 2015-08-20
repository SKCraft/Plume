package com.skcraft.plume.common.extension.module;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.skcraft.plume.common.config.Config;
import com.skcraft.plume.common.config.ConfigFactory;
import lombok.extern.java.Log;
import ninja.leaping.configurate.objectmapping.Setting;
import org.atteo.classindex.ClassFilter;
import org.atteo.classindex.ClassIndex;

import java.util.List;
import java.util.Map;

@Log
class ModuleLoader {

    private final Injector injector;
    private final ConfigFactory configFactory;

    @Inject
    ModuleLoader(Injector injector, ConfigFactory configFactory) {
        this.injector = injector;
        this.configFactory = configFactory;
    }

    public void load() {
        List<Class<?>> modules = Lists.newArrayList(ClassFilter.only().from(ClassIndex.getAnnotated(Module.class)));
        List<Class<?>> loadable = Lists.newArrayList();

        Config<LoaderConfig> config = configFactory.create("modules", LoaderConfig.class);
        config.load();

        Map<String, Boolean> enabledMap = config.get().modules;

        for (Class<?> module : modules) {
            Module annotation = module.getAnnotation(Module.class);

            Boolean enabled = enabledMap.get(annotation.name());

            if (enabled == null) {
                enabled = annotation.enabled();
                if (!annotation.hidden()) {
                    enabledMap.put(annotation.name(), enabled);
                }
            }

            if (enabled) {
                loadable.add(module);
            }
        }

        config.save();

        for (Class<?> module : loadable) {
            log.info("Loading " + Modules.getModuleName(module) + "...");
            injector.getInstance(module);
        }
    }

    private static class LoaderConfig {
        @Setting(value = "modules", comment = "List of modules to auto-load")
        private Map<String, Boolean> modules = Maps.newHashMap();
    }

}
