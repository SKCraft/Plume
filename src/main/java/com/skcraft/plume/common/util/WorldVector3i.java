package com.skcraft.plume.common.util;

import lombok.Data;

@Data
public final class WorldVector3i {

    private final String worldName;
    private final int x;
    private final int y;
    private final int z;

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
