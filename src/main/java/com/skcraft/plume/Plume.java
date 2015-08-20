package com.skcraft.plume;

import com.google.inject.Injector;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.skcraft.plume.common.event.lifecycle.InitializationEvent;
import com.skcraft.plume.common.event.lifecycle.PostInitializationEvent;
import com.skcraft.plume.common.util.FatalError;
import com.skcraft.plume.common.util.logging.Log4jRedirect;
import com.skcraft.plume.common.util.module.PlumeLoader;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.*;
import lombok.extern.java.Log;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Mod(modid = Plume.MODID, name = "Plume", dependencies = "required-after:worldedit", acceptableRemoteVersions = "*")
@Log
public class Plume {

    public static final String MODID = "plume";

    @Instance(MODID)
    public static Plume INSTANCE;

    private Injector injector;

    public EventBus getEventBus() {
        return injector.getInstance(EventBus.class);
    }

    private void handleFatalErrors(List<FatalError> errors) {
        if (!errors.isEmpty()) {
            for (FatalError error : errors) {
                log.log(Level.SEVERE, error.getMessage());
            }

            FMLCommonHandler.instance().exitJava(0, true);
        }
    }

    @EventHandler
    public void onPreInitialization(FMLPreInitializationEvent event) {
        // Hack to redirect log messages
        Logger logger = Logger.getLogger("com.skcraft.plume");
        logger.setUseParentHandlers(false);
        logger.addHandler(new Log4jRedirect(event.getModLog(), "Plume"));

        injector = new PlumeLoader()
                .setDataDir(new File(event.getModConfigurationDirectory(), "plume"))
                .addModule(new PlumeForgeModule())
                .load();

        InitializationEvent initializationEvent = new InitializationEvent();
        getEventBus().post(initializationEvent);
        handleFatalErrors(initializationEvent.getFatalErrors());

        PostInitializationEvent postInitializationEvent = new PostInitializationEvent();
        getEventBus().post(postInitializationEvent);
        handleFatalErrors(postInitializationEvent.getFatalErrors());

        getEventBus().post(event);
    }

    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        getEventBus().post(event);
    }

    @EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        getEventBus().post(event);
    }

    @EventHandler
    public void onServerStopping(FMLServerStoppingEvent event) {
        getEventBus().post(event);
    }

    @EventHandler
    public void onServerStopped(FMLServerStoppedEvent event) {
        getEventBus().post(event);
    }

}
