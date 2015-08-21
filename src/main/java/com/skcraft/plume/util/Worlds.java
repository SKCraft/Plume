package com.skcraft.plume.util;

import net.minecraft.world.World;

public final class Worlds {

    private Worlds() {
    }

    public static String getWorldName(World world) {
        return world.getWorldInfo().getWorldName();
    }

}
