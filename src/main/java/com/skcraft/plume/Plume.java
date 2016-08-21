package com.skcraft.plume;

import com.skcraft.plume.common.util.SharedLocale;
import com.skcraft.plume.common.util.logging.Log4jRedirect;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import lombok.extern.java.Log;

import java.util.Locale;
import java.util.logging.Logger;

@Mod(modid = Plume.MODID, name = "Plume", dependencies = "required-after:worldedit", acceptableRemoteVersions = "*")
@Log
public class Plume {

    public static final String MODID = "plume";
    public static final String CHANNEL_ID = "plume";

    @Instance(MODID)
    public static Plume INSTANCE;

    @SidedProxy(serverSide = "com.skcraft.plume.SharedProxy", clientSide = "com.skcraft.plume.ClientProxy")
    public static SharedProxy PROXY;

    @EventHandler
    public void onPreInitialization(FMLPreInitializationEvent event) throws Exception {
        // Hack to redirect log messages
        Logger logger = Logger.getLogger("com.skcraft.plume");
        logger.setUseParentHandlers(false);
        logger.addHandler(new Log4jRedirect(event.getModLog(), null));

        SharedLocale.loadBundle("com.skcraft.plume.lang.Plume", Locale.getDefault());

        PROXY.onPreInitialization(event);
    }

    @EventHandler
    public void onInitialization(FMLInitializationEvent event) throws Exception {
        PROXY.onInitialization(event);
    }

    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event) throws Exception {
        PROXY.onServerStarting(event);
    }

    @EventHandler
    public void onServerStarted(FMLServerStartedEvent event) throws Exception{
        PROXY.onServerStarted(event);
    }

    @EventHandler
    public void onServerStopping(FMLServerStoppingEvent event) throws Exception {
        PROXY.onServerStopping(event);
    }

    @EventHandler
    public void onServerStopped(FMLServerStoppedEvent event) throws Exception {
        PROXY.onServerStopped(event);
    }

}
