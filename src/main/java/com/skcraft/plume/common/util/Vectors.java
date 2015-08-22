package com.skcraft.plume.common.util;

import com.sk89q.worldedit.Vector2D;

public final class Vectors {

    private Vectors() {
    }

    public static WorldVector3i fromVector2D(String worldName, Vector2D v) {
        return new WorldVector3i(worldName, v.getBlockX(), 0, v.getBlockZ());
    }

}
