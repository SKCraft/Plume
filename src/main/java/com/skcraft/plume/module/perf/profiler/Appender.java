package com.skcraft.plume.module.perf.profiler;

import java.util.List;

public interface Appender {

    List<String> getColumns();

    List<String> getValues(Timing timing);

}
