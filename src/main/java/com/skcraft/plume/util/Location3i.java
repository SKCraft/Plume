package com.skcraft.plume.util;

import com.skcraft.plume.common.util.WorldVector3i;
import lombok.Data;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;

import static com.google.common.base.Preconditions.checkNotNull;

@Data
public class Location3i {

    private final World world;
    private final int x;
    private final int y;
    private final int z;

    public Location3i(World world, int x, int y, int z) {
        checkNotNull(world, "world");
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Location3i setX(int x) {
        return new Location3i(world, x, y, z);
    }

    public Location3i setY(int y) {
        return new Location3i(world, x, y, z);
    }

    public Location3i setZ(int z) {
        return new Location3i(world, x, y, z);
    }

    public Location3i add(int x1, int y1, int z1) {
        return new Location3i(world, x + x1, y + y1, z + z1);
    }

    public Location3i add(Location3i other) {
        return new Location3i(world, x + other.getX(), y + other.getY(), z + other.getZ());
    }

    public Location3i subtract(int x1, int y1, int z1) {
        return new Location3i(world, x - x1, y - y1, z - z1);
    }

    public Location3i subtract(Location3i other) {
        return new Location3i(world, x - other.getX(), y - other.getY(), z - other.getZ());
    }

    public Location3i multiply(int m) {
        return new Location3i(world, x * m, y * m, z * m);
    }

    public double distanceSq(Location3i other) {
        return Math.pow(getX() - other.getX(), 2) + Math.pow(getY() - other.getY(), 2) + Math.pow(getZ() - other.getZ(), 2);
    }

    public double distance(Location3i other) {
        return Math.sqrt(distanceSq(other));
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public WorldVector3i toWorldVector() {
        return new WorldVector3i(Worlds.getWorldId(world), x, y, z);
    }

    @Override
    public String toString() {
        return "{" + world.getWorldInfo().getWorldName() + ":" + x + "," + y + "," + z + "}";
    }

    public static Location3i fromBlockSnapshot(BlockSnapshot snapshot) {
        checkNotNull(snapshot, "snapshot");
        return new Location3i(snapshot.world, snapshot.x, snapshot.y, snapshot.z);
    }

    public static Location3i fromObjectPosition(World world, MovingObjectPosition target) {
        checkNotNull(target, "target");
        return new Location3i(world, target.blockX, target.blockY, target.blockZ);
    }

    public Location3d toCenteredLocation3d() {
        return new Location3d(world, x + 0.5, y, z + 0.5);
    }

}
