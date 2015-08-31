package com.skcraft.plume.common.util;

import lombok.Data;

import java.io.Serializable;

@Data
public final class WorldVector3i implements Serializable {

    private static final long serialVersionUID = 2952075843661465400L;

    private final String worldName;
    private final int x;
    private final int y;
    private final int z;

    public WorldVector3i add(int x, int y, int z) {
        return new WorldVector3i(worldName, this.x + x, this.y + y, this.z + z);
    }

    public WorldVector3i sub(int x, int y, int z) {
        return new WorldVector3i(worldName, this.x - x, this.y - y, this.z - z);
    }

    public WorldVector3i mult(int x, int y, int z) {
        return new WorldVector3i(worldName, this.x * x, this.y * y, this.z * z);
    }

    public WorldVector3i div(int x, int y, int z) {
        return new WorldVector3i(worldName, this.x / x, this.y / y, this.z / z);
    }

    @Override
    public String toString() {
        return "{" + worldName + ":" + x + "," + y + "," + z + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WorldVector3i)) {
            return false;
        }

        WorldVector3i other = (WorldVector3i) obj;
        return getX() == other.getX() && getY() == other.getY() && getZ() == other.getZ();
    }

    @Override
    public int hashCode() {
        return getX() + getZ() << 16 + getY() << 24;
    }

}
