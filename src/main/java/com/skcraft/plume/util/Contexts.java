package com.skcraft.plume.util;

import com.skcraft.plume.common.auth.Context;
import net.minecraft.entity.player.EntityPlayer;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Contexts {

    private Contexts() {
    }

    public static Context.Builder update(Context.Builder builder, EntityPlayer player) {
        checkNotNull(builder, "builder");
        checkNotNull(player, "player");
        builder.put("world", player.worldObj.getWorldInfo().getWorldName());
        return builder;
    }

}
