package com.skcraft.plume.common.util.module;

import com.google.common.collect.Lists;
import com.google.inject.*;
import com.google.inject.Module;

import java.io.File;
import java.io.IOException;
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

    public Injector load() throws IOException {
        dataDir.mkdirs();

        List<Module> modules = Lists.newArrayList();
        modules.add(new PlumeModule(dataDir));
        modules.addAll(this.modules);
        Injector injector = Guice.createInjector(modules);
        injector.getInstance(ModuleLoader.class).load();
        return injector;
    }

}
