package com.skcraft.plume;

import com.google.inject.Injector;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.skcraft.plume.common.event.lifecycle.InitializationEvent;
import com.skcraft.plume.common.event.lifecycle.PostInitializationEvent;
import com.skcraft.plume.common.extension.module.PlumeLoader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.*;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(modid = Plume.MODID, name = "Plume", dependencies = "required-after:worldedit")
public class Plume {
    
    public static final String MODID = "plume";

    @Instance(MODID)
    public static Plume INSTANCE;
    @SidedProxy(serverSide = "com.skcraft.plume.CommonProxy", clientSide = "com.skcraft.plume.ClientProxy")
    public static CommonProxy PROXY;

    private Injector injector;
    private Logger logger;

    public EventBus getEventBus() {
        return injector.getInstance(EventBus.class);
    }

    public Logger getLogger() {
        return logger;
    }

    @EventHandler
    public void onPreInitialization(FMLPreInitializationEvent event) {
        logger = event.getModLog();

        injector = new PlumeLoader()
                .setDataDir(new File(event.getModConfigurationDirectory(), "plume"))
                .addModule(new PlumeForgeModule())
                .load();

        getEventBus().post(new InitializationEvent());
        getEventBus().post(new PostInitializationEvent());
        getEventBus().post(event);
        PROXY.preInit(event);
    }

    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        getEventBus().post(event);
        PROXY.serverStarting(event);
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
