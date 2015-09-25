package com.skcraft.plume.common.util.module;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.skcraft.plume.common.util.config.ConfigFactory;
import lombok.extern.java.Log;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.atteo.classindex.ClassFilter;
import org.atteo.classindex.ClassIndex;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Log
class ModuleLoader {

    private final File dataDir;
    private final ConfigFactory configFactory;
    private final List<com.google.inject.Module> additionalInjectorModules;

    ModuleLoader(File dataDir, ConfigFactory configFactory, List<com.google.inject.Module> additionalInjectorModules) {
        this.dataDir = dataDir;
        this.configFactory = configFactory;
        this.additionalInjectorModules = additionalInjectorModules;
    }

    public Injector load() throws LoaderException {
        List<com.google.inject.Module> loadedInjectorModules = Lists.newArrayList();
        loadedInjectorModules.add(new PlumeModule(dataDir));
        loadedInjectorModules.addAll(additionalInjectorModules);

        List<Class<?>> modules = Lists.newArrayList(ClassFilter.only().from(ClassIndex.getAnnotated(Module.class)));
        List<Class<?>> loadable = Lists.newArrayList();

        ConfigurationLoader<CommentedConfigurationNode> loader = configFactory.createLoader("modules");
        CommentedConfigurationNode config;
        try {
            config = loader.load();
        } catch (IOException e) {
            throw new LoaderException("Failed to read the configuration file specifying enabled Plume modules", e);
        }

        CommentedConfigurationNode enabledNode = config.getNode("modules");
        enabledNode.setComment("List of modules to auto-load");
        Map<?, ?> map = enabledNode.getChildrenMap();

        for (Class<?> module : modules) {
            Module annotation = module.getAnnotation(Module.class);
            boolean enabled = annotation.enabled();

            if (map.containsKey(annotation.name()) || !annotation.hidden()) {
                CommentedConfigurationNode moduleNode = enabledNode.getNode(annotation.name());
                moduleNode.setComment(Strings.emptyToNull(annotation.desc()));

                if (map.containsKey(annotation.name())) {
                    enabled = moduleNode.getBoolean();
                }

                moduleNode.setValue(enabled);
            }

            if (enabled) {
                for (Class<? extends com.google.inject.Module> injectorModule : annotation.injectorModule()) {
                    log.info("Found injector module " + injectorModule.getName() + " from " + Modules.getModuleName(module));
                    try {
                        loadedInjectorModules.add(injectorModule.newInstance());
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new LoaderException("Failed to instanciate " + injectorModule.getClass() + " from " + Modules.getModuleName(module));
                    }
                }

                loadable.add(module);
            }
        }

        try {
            loader.save(config);
        } catch (IOException e) {
            throw new LoaderException("Failed to save the configuration file specifying enabled Plume modules", e);
        }

        Injector injector = Guice.createInjector(loadedInjectorModules);

        for (Class<?> module : loadable) {
            log.info("Loading " + Modules.getModuleName(module) + "...");
            injector.getInstance(module);
        }

        return injector;
    }

}
