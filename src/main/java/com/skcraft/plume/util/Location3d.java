package com.skcraft.plume.util;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public final class Location3d {

    private final World world;
    private final double x;
    private final double y;
    private final double z;

    public Location3d(World world, double x, double y, double z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public World getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    @Override
    public String toString() {
        return "{" + world.getWorldInfo().getWorldName() + ":" + x + "," + y + "," + z + "}";
    }

    public static Location3d fromEntity(Entity entity) {
        return new Location3d(entity.worldObj, entity.posX, entity.posY, entity.posZ);
    }

}
