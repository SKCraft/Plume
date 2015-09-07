package com.skcraft.plume.util;

import com.google.common.base.Preconditions;
import lombok.Data;

import static com.google.common.base.Preconditions.checkNotNull;

@Data
public class WeightedEntry<T> implements Comparable<WeightedEntry<?>> {

    private final T entry;
    private final double weight;

    public WeightedEntry(T entry, double weight) {
        checkNotNull(entry, "item");
        Preconditions.checkNotNull(weight, "weight");
        this.entry = entry;
        this.weight = weight;
    }

    @Override
    public int compareTo(WeightedEntry o) {
        if (weight > o.getWeight()) {
            return -1;
        } else if (weight < o.getWeight()) {
            return 1;
        } else {
            return 0;
        }
    }

}
