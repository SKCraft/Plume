package com.skcraft.plume.module.perf.profiler;

import lombok.Getter;

import java.util.List;

class CountableStackTrace implements Comparable<CountableStackTrace> {

    @Getter private final List<StackTraceElement> stackTrace;
    @Getter private int count = 1;

    CountableStackTrace(List<StackTraceElement> stackTrace) {
        this.stackTrace = stackTrace;
    }

    public void increment() {
        count++;
    }

    @Override
    public int compareTo(CountableStackTrace o) {
        if (count > o.count) {
            return -1;
        } else if (count < o.count) {
            return 1;
        } else {
            return 0;
        }
    }

}
