package com.skcraft.plume.event.report;

import com.google.common.collect.Lists;
import lombok.Getter;

import java.util.Collection;
import java.util.List;

public class DecorateReportEvent {

    @Getter private final Collection<? extends Row> rows;
    @Getter private final List<Decorator> decorators = Lists.newArrayList();

    public DecorateReportEvent(Collection<? extends Row> rows) {
        this.rows = rows;
    }

}
