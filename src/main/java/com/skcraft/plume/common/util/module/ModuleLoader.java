package com.skcraft.plume.common.util.module;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.skcraft.plume.common.util.config.ConfigFactory;
import lombok.extern.java.Log;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.atteo.classindex.ClassFilter;
import org.atteo.classindex.ClassIndex;

import java.io.IOException;
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

    public void load() throws IOException {
        List<Class<?>> modules = Lists.newArrayList(ClassFilter.only().from(ClassIndex.getAnnotated(Module.class)));
        List<Class<?>> loadable = Lists.newArrayList();

        ConfigurationLoader<CommentedConfigurationNode> loader = configFactory.createLoader("modules");
        CommentedConfigurationNode config = loader.load();

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
                loadable.add(module);
            }
        }

        loader.save(config);

        for (Class<?> module : loadable) {
            log.info("Loading " + Modules.getModuleName(module) + "...");
            injector.getInstance(module);
        }
    }

}
