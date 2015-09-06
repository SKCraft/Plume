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

    public Location3d setX(int x) {
        return new Location3d(world, x, y, z);
    }

    public Location3d setY(int y) {
        return new Location3d(world, x, y, z);
    }

    public Location3d setZ(int z) {
        return new Location3d(world, x, y, z);
    }

    public Location3d add(Location3d other) {
        return new Location3d(world, x + other.getX(), y + other.getY(), z + other.getZ());
    }

    public Location3d subtract(Location3d other) {
        return new Location3d(world, x - other.getX(), y - other.getY(), z - other.getZ());
    }

    public Location3d multiply(int m) {
        return new Location3d(world, x * m, y * m, z * m);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Location3d unit() {
        double length = length();
        return new Location3d(world, x / length, y / length, z / length);
    }

    @Override
    public String toString() {
        return "{" + world.getWorldInfo().getWorldName() + ":" + x + "," + y + "," + z + "}";
    }

    public static Location3d fromEntity(Entity entity) {
        return new Location3d(entity.worldObj, entity.posX, entity.posY, entity.posZ);
    }

}
