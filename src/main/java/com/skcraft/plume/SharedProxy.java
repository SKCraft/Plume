package com.skcraft.plume;

import com.google.common.reflect.TypeToken;
import com.google.inject.Injector;
import com.skcraft.plume.common.event.lifecycle.InitializationEvent;
import com.skcraft.plume.common.event.lifecycle.InitializationVerifyEvent;
import com.skcraft.plume.common.event.lifecycle.PostInitializationEvent;
import com.skcraft.plume.common.util.FatalError;
import com.skcraft.plume.common.util.config.SetTypeSerializer;
import com.skcraft.plume.common.util.event.EventBus;
import com.skcraft.plume.common.util.module.LoaderException;
import com.skcraft.plume.common.util.module.PlumeLoader;
import com.skcraft.plume.network.PlumePacketHandler;
import com.skcraft.plume.util.config.ItemStackTypeSerializer;
import com.skcraft.plume.util.config.SingleItemMatcherTypeSerializer;
import com.skcraft.plume.util.inventory.SingleItemMatcher;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.NetworkRegistry;
import lombok.Getter;
import lombok.extern.java.Log;
import net.minecraft.item.ItemStack;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

@Log
public class SharedProxy {

    private FMLEventChannel eventChannel;
    private Injector injector;
    @Getter
    private PlumePacketHandler packetHandler;

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

    public void onPreInitialization(FMLPreInitializationEvent event) throws LoaderException {
        TypeSerializers.getDefaultSerializers().registerType(new TypeToken<Set<?>>() {}, new SetTypeSerializer());
        TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(ItemStack.class), new ItemStackTypeSerializer());
        TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(SingleItemMatcher.class), new SingleItemMatcherTypeSerializer());

        injector = new PlumeLoader()
                .setDataDir(new File(event.getModConfigurationDirectory(), "plume"))
                .addModule(new PlumeForgeModule())
                .load();

        InitializationEvent initializationEvent = new InitializationEvent();
        //noinspection ConstantConditions
        getEventBus().post(initializationEvent);
        handleFatalErrors(initializationEvent.getFatalErrors());

        InitializationVerifyEvent initializationVerifyEvent = new InitializationVerifyEvent();
        getEventBus().post(initializationVerifyEvent);
        handleFatalErrors(initializationVerifyEvent.getFatalErrors());

        getEventBus().post(new PostInitializationEvent());

        getEventBus().post(event);
    }

    public void onInitialization(FMLInitializationEvent event) {
        eventChannel = NetworkRegistry.INSTANCE.newEventDrivenChannel(Plume.CHANNEL_ID);
        packetHandler = new PlumePacketHandler(eventChannel);
        eventChannel.register(packetHandler);
    }

    public void onServerStarting(FMLServerStartingEvent event) {
        getEventBus().post(event);
    }

    public void onServerStarted(FMLServerStartedEvent event) {
        getEventBus().post(event);
    }

    public void onServerStopping(FMLServerStoppingEvent event) {
        getEventBus().post(event);
    }

    public void onServerStopped(FMLServerStoppedEvent event) {
        getEventBus().post(event);
    }

}
