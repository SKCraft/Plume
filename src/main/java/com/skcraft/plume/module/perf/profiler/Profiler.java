package com.skcraft.plume.module.perf.profiler;

import com.google.common.collect.Maps;
import com.skcraft.plume.common.util.Stopwatch;
import lombok.Getter;

import java.util.Collection;
import java.util.Map;

public class Profiler {

    @Getter
    private final ReusableStopwatch stopwatch = new ReusableStopwatch();
    private Map<Timing, Timing> data = Maps.newHashMap();

    public Collection<Timing> getTimings() {
        return data.values();
    }

    public void increment(String className, String world, int x, int y, int z, long time) {
        Timing timing = new Timing(className, world, x, y, z, time);
        Timing found = data.get(timing);

        if (found == null) {
            data.put(timing, timing);
            found = timing;
        }

        found.increment(time);
    }

    public class ReusableStopwatch implements Stopwatch {
        private String className;
        private String worldName;
        private int x;
        private int y;
        private int z;
        private long start;

        private ReusableStopwatch() {
        }

        public void set(String worldName, String className, int x, int y, int z) {
            this.worldName = worldName;
            this.className = className;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public void start() {
            start = System.nanoTime();
        }

        @Override
        public void stop() {
            long end = System.nanoTime();
            increment(className, worldName, x, y, z, end - start);
        }
    }

}
