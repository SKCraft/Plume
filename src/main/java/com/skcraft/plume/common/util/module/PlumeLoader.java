package com.skcraft.plume.common.util.module;

import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.skcraft.plume.common.util.config.ConfigFactory;

import java.io.File;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class PlumeLoader {

    private File dataDir = new File(".");
    private final List<com.google.inject.Module> modules = Lists.newArrayList();

    public PlumeLoader setDataDir(File dataDir) {
        checkNotNull(dataDir, "dataDir");
        this.dataDir = dataDir;
        return this;
    }

    public PlumeLoader addModule(com.google.inject.Module module) {
        checkNotNull(module, "module");
        modules.add(module);
        return this;
    }

    public Injector load() throws LoaderException {
        dataDir.mkdirs();

        ModuleLoader loader = new ModuleLoader(dataDir, new ConfigFactory(dataDir), modules);
        return loader.load();
    }

}
