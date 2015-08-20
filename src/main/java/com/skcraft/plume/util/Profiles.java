package com.skcraft.plume.util;

import com.mojang.authlib.GameProfile;
import com.skcraft.plume.common.UserId;
import net.minecraft.entity.player.EntityPlayer;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Profiles {

    private Profiles() {
    }

    public static UserId fromProfile(GameProfile gameProfile) {
        checkNotNull(gameProfile, "gameProfile");
        return new UserId(gameProfile.getId(), gameProfile.getName());
    }

    public static UserId fromPlayer(EntityPlayer player) {
        return fromProfile(player.getGameProfile());
    }

}
