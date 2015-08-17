package com.skcraft.plume.event.network;

import com.mojang.authlib.GameProfile;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.server.network.NetHandlerLoginServer;

public class PlayerAuthenticateEvent extends Event {

    private final GameProfile profile;
    private final NetHandlerLoginServer netHandler;

    public PlayerAuthenticateEvent(GameProfile profile, NetHandlerLoginServer netHandler) {
        this.profile = profile;
        this.netHandler = netHandler;
    }

    public GameProfile getProfile() {
        return profile;
    }

    public NetHandlerLoginServer getNetHandler() {
        return netHandler;
    }
}
