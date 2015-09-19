package com.skcraft.plume.common.util;

public final class Metrics {

    private Metrics() {
    }

    public static String escape(String name) {
        return name.replace(".", "_");
    }

}
