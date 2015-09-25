package com.skcraft.plume;

import cpw.mods.fml.common.event.*;

public interface Proxy {

    void onPreInitialization(FMLPreInitializationEvent event) throws Exception;

    void onServerStarting(FMLServerStartingEvent event) throws Exception;

    void onServerStarted(FMLServerStartedEvent event) throws Exception;

    void onServerStopping(FMLServerStoppingEvent event) throws Exception;

    void onServerStopped(FMLServerStoppedEvent event) throws Exception;

}
