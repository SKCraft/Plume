package com.skcraft.plume.common.util;

import javax.annotation.Nullable;

public interface ObjectCache<K, V> {

    @Nullable
    V load(K key);

    @Nullable
    V get(K key);

    @Nullable
    V getIfPresent(K key);

    void refreshAll();

}
