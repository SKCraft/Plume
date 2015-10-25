package com.skcraft.plume.module.perf.profiler;

import com.skcraft.plume.event.report.Row;
import lombok.Getter;

@Getter
public class Timing implements Row {

    private final String className;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private long time;
    private final int hashCode;

    Timing(String className, String world, int x, int y, int z, long time) {
        this.className = className;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.time = time;
        this.hashCode = generateHashCode();
    }

    public void increment(long time) {
        this.time += time;
    }

    @Override
    public int getChunkX() {
        return x >> 4;
    }

    @Override
    public int getChunkZ() {
        return z >> 4;
    }

    private int generateHashCode() {
        int result = className.hashCode();
        result = 31 * result + world.hashCode();
        result = 31 * result + x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Timing timing = (Timing) o;

        if (x != timing.x) return false;
        if (y != timing.y) return false;
        if (z != timing.z) return false;
        if (!className.equals(timing.className)) return false;
        if (!world.equals(timing.world)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

}
