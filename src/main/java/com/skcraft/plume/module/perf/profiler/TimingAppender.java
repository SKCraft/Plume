package com.skcraft.plume.module.perf.profiler;

import com.google.common.collect.Lists;

import java.util.List;

class TimingAppender implements Appender {
    
    @Override
    public List<String> getColumns() {
        return Lists.newArrayList("World", "X", "Y", "Z", "Class", "Time");
    }

    @Override
    public List<String> getValues(Timing timing) {
        return Lists.newArrayList(
                timing.getWorld(),
                String.valueOf(timing.getX()),
                String.valueOf(timing.getY()),
                String.valueOf(timing.getZ()),
                timing.getClassName(),
                String.valueOf(timing.getTime() / (double) 1000000)
        );
    }
    
}
