package com.skcraft.plume.common.util.pagination;

import javax.annotation.Nullable;
import java.util.List;

public interface Page<T> extends List<T> {

    Pagination<T> getPagination();

    int getAbsoluteIndex(int index);

    int page();

    @Nullable
    Page<T> previous();

    @Nullable
    Page<T> next();

    int size();

}
