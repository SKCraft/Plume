package com.skcraft.plume.common.util;

public class XorRandom {

    private long x = 0;

    public XorRandom() {
        this(System.nanoTime());
    }

    public XorRandom(long x) {
        this.x = x;
    }

    public long nextLong() {
        x ^= (x << 21);
        x ^= (x >>> 35);
        x ^= (x << 4);
        return x;
    }

}
