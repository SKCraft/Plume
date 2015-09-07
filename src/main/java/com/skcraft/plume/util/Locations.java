package com.skcraft.plume.util;

import com.skcraft.plume.common.util.WorldVector3i;
import net.minecraft.entity.Entity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

public final class Locations {

    private Locations() {
    }

    public static Location3d getLocation3d(Entity entity) {
        return new Location3d(entity.worldObj, entity.posX, entity.posY, entity.posZ);
    }

    public static Location3i getLocation3i(Entity entity) {
        return new Location3i(entity.worldObj, (int) entity.posX, (int) entity.posY, (int) entity.posZ);
    }

    public static WorldVector3i getWorldVector3i(Entity entity) {
        return getLocation3i(entity).toWorldVector();
    }

    public static Location3i getLocation3i(World world, ChunkCoordinates coords) {
        return new Location3i(world, coords.posX, coords.posY, coords.posZ);
    }

    public static Location3d getLocation3d(World world, ChunkCoordinates coords) {
        return new Location3d(world, coords.posX, coords.posY, coords.posZ);
    }

}
