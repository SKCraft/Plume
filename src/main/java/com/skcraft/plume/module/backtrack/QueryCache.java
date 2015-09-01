package com.skcraft.plume.module.backtrack;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Singleton;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.journal.Record;
import com.skcraft.plume.common.util.pagination.ListPagination;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

@Singleton
public class QueryCache {

    private final Cache<UserId, ListPagination<Record>> lastQuery = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).build();

    public void put(UserId key, ListPagination<Record> value) {
        lastQuery.put(key, value);
    }

    public void invalidate(UserId key) {
        lastQuery.invalidate(key);
    }

    @Nullable
    public ListPagination<Record> getIfPresent(UserId key) {
        return lastQuery.getIfPresent(key);
    }

}
