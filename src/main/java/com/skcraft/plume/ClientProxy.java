package com.skcraft.plume;

import com.skcraft.plume.common.util.module.LoaderException;
import net.minecraftforge.fml.common.event.*;

public class ClientProxy extends SharedProxy {

    private boolean shouldLoadModules() {
        return System.getProperty("plume.enableModulesOnClient", "false").equals("true");
    }

    @Override
    public void onPreInitialization(FMLPreInitializationEvent event) throws LoaderException {
        if (shouldLoadModules()) {
            super.onPreInitialization(event);
        }
    }

    @Override
    public void onServerStarting(FMLServerStartingEvent event) {
        if (shouldLoadModules()) {
            super.onServerStarting(event);
        }
    }

    @Override
    public void onServerStarted(FMLServerStartedEvent event) {
        if (shouldLoadModules()) {
            super.onServerStarted(event);
        }
    }

    @Override
    public void onServerStopping(FMLServerStoppingEvent event) {
        if (shouldLoadModules()) {
            super.onServerStopping(event);
        }
    }

    @Override
    public void onServerStopped(FMLServerStoppedEvent event) {
        if (shouldLoadModules()) {
            super.onServerStopped(event);
        }
    }

}
