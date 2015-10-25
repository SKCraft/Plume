package com.skcraft.plume.event.report;

import java.util.List;

public interface Decorator {

    List<String> getColumns();

    List<String> getValues(Row entry);

}
