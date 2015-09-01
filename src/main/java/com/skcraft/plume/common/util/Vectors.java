package com.skcraft.plume.common.util;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.skcraft.plume.util.Location3d;
import com.skcraft.plume.util.Location3i;
import com.skcraft.plume.util.NoSuchWorldException;
import com.skcraft.plume.util.Worlds;
import net.minecraft.world.World;

public final class Vectors {

    private Vectors() {
    }

    public static WorldVector3i toWorldVector3i(String worldName, Vector2D v) {
        return new WorldVector3i(worldName, v.getBlockX(), 0, v.getBlockZ());
    }

    public static Vector toVector(WorldVector3i v) {
        return new Vector(v.getX(), v.getY(), v.getZ());
    }

    public static Location3d toCornerLocation3d(WorldVector3i position) throws NoSuchWorldException {
        World world = Worlds.getWorldFromId(position.getWorldId());
        return new Location3d(world, position.getX(), position.getY(), position.getZ());
    }

    public static Location3d toCenteredLocation3d(WorldVector3i position) throws NoSuchWorldException {
        World world = Worlds.getWorldFromId(position.getWorldId());
        return new Location3d(world, position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
    }

    public static Location3i toLocation3i(WorldVector3i position) throws NoSuchWorldException {
        World world = Worlds.getWorldFromId(position.getWorldId());
        return new Location3i(world, position.getX(), position.getY(), position.getZ());
    }

}
