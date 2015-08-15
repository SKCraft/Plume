package com.skcraft.plume;

import com.sk89q.worldedit.util.eventbus.EventBus;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = Plume.MODID, name = "Plume", dependencies = "required-after:worldedit")
public class Plume {
    
    public static final String MODID = "plume";

    @Instance(MODID)
    public static Plume INSTANCE;
    @SidedProxy(serverSide = "com.skcraft.plume.CommonProxy", clientSide = "com.skcraft.plume.ClientProxy")
    public static CommonProxy PROXY;

    private final EventBus eventBus = new EventBus();
    private Logger logger;

    public EventBus getEventBus() {
        return eventBus;
    }

    public Logger getLogger() {
        return logger;
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        PROXY.preInit(event);
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        PROXY.serverStarting(event);
    }

}
