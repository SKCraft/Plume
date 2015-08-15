package com.skcraft.plume.common.util.cache;

import com.google.common.cache.CacheLoader;

import java.util.concurrent.TimeUnit;

public class ManagedCacheBuilder<K, V> {

    private int expireDelay = 5;
    private TimeUnit expireTimeUnit = TimeUnit.MINUTES;

    private ManagedCacheBuilder() {
    }

    public ManagedCacheBuilder<K, V> expireAfterAccess(int expireDelay, TimeUnit expireTimeUnit) {
        this.expireDelay = expireDelay;
        this.expireTimeUnit = expireTimeUnit;
        return this;
    }

    public <K1 extends K, V1 extends V> ManagedCache<K1, V1> build(CacheLoader<K1, V1> loader) {
        return new GuavaManagedCache<>(loader, expireDelay, expireTimeUnit);
    }

    public static <K, V> ManagedCacheBuilder<K, V> newBuilder() {
        return new ManagedCacheBuilder<>();
    }

}
