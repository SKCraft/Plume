package com.skcraft.plume.common.util.pagination;

import javax.annotation.Nullable;

public interface Pagination<T> extends Iterable<Page<T>> {

    int getPerPage();

    int firstPage();

    int lastPage();

    @Nullable
    Page<T> first();

    @Nullable
    Page<T> last();

    boolean has(int page);

    @Nullable
    Page<T> at(int page);

    boolean isEmpty();

    int itemCount();

    int pageCount();

}
