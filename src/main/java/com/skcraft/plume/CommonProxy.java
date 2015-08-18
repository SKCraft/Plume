package com.skcraft.plume;

import com.sk89q.worldedit.util.eventbus.EventBus;
import com.skcraft.plume.listener.DebuggingListener;
import com.skcraft.plume.listener.EventAbstractionListener;
import com.skcraft.plume.listener.RobotAbstractionListener;
import com.skcraft.plume.listener.UserManagementListener;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.common.MinecraftForge;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
    }

    public void serverStarting(FMLServerStartingEvent event) {
        EventBus eventBus = Plume.INSTANCE.getEventBus();
        eventBus.register(new DebuggingListener(Plume.INSTANCE.getLogger()));
        MinecraftForge.EVENT_BUS.register(new EventAbstractionListener(eventBus));
        MinecraftForge.EVENT_BUS.register(new RobotAbstractionListener(eventBus));
        MinecraftForge.EVENT_BUS.register(new UserManagementListener());
    }

}
