package com.skcraft.plume.module.perf.profiler;

import com.google.common.collect.Lists;
import lombok.Getter;

import java.util.Collection;
import java.util.List;

public class CollectAppendersEvent {

    @Getter private final Collection<Timing> timings;
    @Getter private final List<Appender> appenders = Lists.newArrayList();

    public CollectAppendersEvent(Collection<Timing> timings) {
        this.timings = timings;
    }

}
