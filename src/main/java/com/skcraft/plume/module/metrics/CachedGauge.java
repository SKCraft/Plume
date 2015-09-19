package com.skcraft.plume.module.metrics;

import com.codahale.metrics.Gauge;

public class CachedGauge<T> implements Gauge<T> {

    private T value;

    @Override
    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

}
