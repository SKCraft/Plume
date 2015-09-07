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

    public Location3d setX(double x) {
        return new Location3d(world, x, y, z);
    }

    public Location3d setY(double y) {
        return new Location3d(world, x, y, z);
    }

    public Location3d setZ(double z) {
        return new Location3d(world, x, y, z);
    }

    public Location3d add(double x1, double y1, double z1) {
        return new Location3d(world, x + x1, y + y1, z + z1);
    }

    public Location3d add(Location3d other) {
        return new Location3d(world, x + other.getX(), y + other.getY(), z + other.getZ());
    }

    public Location3d subtract(double x1, double y1, double z1) {
        return new Location3d(world, x - x1, y - y1, z - z1);
    }

    public Location3d subtract(Location3d other) {
        return new Location3d(world, x - other.getX(), y - other.getY(), z - other.getZ());
    }

    public Location3d multiply(double m) {
        return new Location3d(world, x * m, y * m, z * m);
    }

    public double distanceSq(Location3d other) {
        return Math.pow(getX() - other.getX(), 2) + Math.pow(getY() - other.getY(), 2) + Math.pow(getZ() - other.getZ(), 2);
    }

    public double distance(Location3d other) {
        return Math.sqrt(distanceSq(other));
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

    public Location3i toFloorLocation3i() {
        return new Location3i(world, (int) x, (int) y, (int) z);
    }

}
