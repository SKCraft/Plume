package com.skcraft.plume.module.perf.profiler;

import com.google.common.collect.Maps;
import com.skcraft.plume.common.util.Stopwatch;
import com.skcraft.plume.event.tick.EntityTickEvent;
import com.skcraft.plume.event.tick.TileEntityTickEvent;
import com.skcraft.plume.util.Worlds;
import com.skcraft.plume.util.profiling.Profiler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;

import java.util.Collection;
import java.util.Map;

public class TickProfiler implements Profiler {

    @Getter
    private final ReusableStopwatch stopwatch = new ReusableStopwatch();
    private Map<Timing, Timing> data = Maps.newHashMap();

    public Collection<Timing> getTimings() {
        return data.values();
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent event) {
        Entity entity = event.getEntity();
        stopwatch.set(Worlds.getWorldId(event.getWorld()), entity.getClass().getName(), (int) entity.posX, (int) entity.posY, (int) entity.posZ);
        event.getStopwatches().add(stopwatch);
    }

    @SubscribeEvent
    public void onTileEntityTick(TileEntityTickEvent event) {
        TileEntity tileEntity = event.getTileEntity();
        stopwatch.set(Worlds.getWorldId(event.getWorld()), tileEntity.getClass().getName(), tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
        event.getStopwatches().add(stopwatch);
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

    @Override
    public void start() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void stop() {
        MinecraftForge.EVENT_BUS.unregister(this);
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
