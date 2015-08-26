package com.skcraft.plume;

import com.google.common.reflect.TypeToken;
import com.google.inject.Injector;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.skcraft.plume.common.event.lifecycle.InitializationEvent;
import com.skcraft.plume.common.event.lifecycle.InitializationVerifyEvent;
import com.skcraft.plume.common.event.lifecycle.PostInitializationEvent;
import com.skcraft.plume.common.util.FatalError;
import com.skcraft.plume.common.util.SharedLocale;
import com.skcraft.plume.common.util.config.SetTypeSerializer;
import com.skcraft.plume.common.util.logging.Log4jRedirect;
import com.skcraft.plume.common.util.module.PlumeLoader;
import com.skcraft.plume.util.config.ItemStackTypeSerializer;
import com.skcraft.plume.util.config.SingleItemMatcherTypeSerializer;
import com.skcraft.plume.util.inventory.SingleItemMatcher;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.*;
import lombok.extern.java.Log;
import net.minecraft.item.ItemStack;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

        SharedLocale.loadBundle("com.skcraft.plume.lang.Plume", Locale.getDefault());

        TypeSerializers.getDefaultSerializers().registerType(new TypeToken<Set<?>>() {}, new SetTypeSerializer());
        TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(ItemStack.class), new ItemStackTypeSerializer());
        TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(SingleItemMatcher.class), new SingleItemMatcherTypeSerializer());

        injector = new PlumeLoader()
                .setDataDir(new File(event.getModConfigurationDirectory(), "plume"))
                .addModule(new PlumeForgeModule())
                .load();

        InitializationEvent initializationEvent = new InitializationEvent();
        getEventBus().post(initializationEvent);
        handleFatalErrors(initializationEvent.getFatalErrors());

        InitializationVerifyEvent initializationVerifyEvent = new InitializationVerifyEvent();
        getEventBus().post(initializationVerifyEvent);
        handleFatalErrors(initializationVerifyEvent.getFatalErrors());

        getEventBus().post(new PostInitializationEvent());

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
