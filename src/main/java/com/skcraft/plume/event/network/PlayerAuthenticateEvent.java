package com.skcraft.plume.event.network;

import com.mojang.authlib.GameProfile;
import cpw.mods.fml.common.eventhandler.Event;

public class PlayerAuthenticateEvent extends Event {

    private final GameProfile profile;

    public PlayerAuthenticateEvent(GameProfile profile) {
        this.profile = profile;
    }

    public GameProfile getProfile() {
        return profile;
    }
}
