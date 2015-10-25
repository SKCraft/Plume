package com.skcraft.plume.module.perf.profiler;

import com.google.common.collect.Lists;
import com.skcraft.plume.event.report.Decorator;
import com.skcraft.plume.event.report.Row;

import java.util.List;

class TimingDecorator implements Decorator {
    
    @Override
    public List<String> getColumns() {
        return Lists.newArrayList("World", "X", "Y", "Z", "Class", "Time");
    }

    @Override
    public List<String> getValues(Row entry) {
        Timing timing = (Timing) entry;
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
