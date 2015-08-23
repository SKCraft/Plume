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

    public WorldVector3i toWorldVector() {
        return new WorldVector3i(Worlds.getWorldName(world), x, y, z);
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

}
