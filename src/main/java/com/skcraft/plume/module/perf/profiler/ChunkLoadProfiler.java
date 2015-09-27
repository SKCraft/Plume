package com.skcraft.plume.module.perf.profiler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.skcraft.plume.event.world.ChunkLoadRequestEvent;
import com.skcraft.plume.util.profiling.Profiler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import lombok.Getter;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;
import java.util.Map;

public class ChunkLoadProfiler implements Profiler {

    @Getter
    private final Map<List<StackTraceElement>, CountableStackTrace> counts = Maps.newHashMap();

    @SubscribeEvent
    public void onChunkLoadRequestEvent(ChunkLoadRequestEvent event) {
        List<StackTraceElement> stackTrace = Lists.newArrayList(new Throwable().getStackTrace());
        ChunkLoadRequestEvent.stripStackTrace(stackTrace);
        CountableStackTrace countable = counts.get(stackTrace);
        if (countable == null) {
            countable = new CountableStackTrace(stackTrace);
            counts.put(stackTrace, countable);
        } else {
            countable.increment();
        }
    }

    @Override
    public void start() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void stop() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

}
