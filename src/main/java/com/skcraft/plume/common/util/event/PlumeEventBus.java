package com.skcraft.plume.common.util.event;

public final class PlumeEventBus {

    public static final EventBus INSTANCE = new SimpleEventBus();

    private PlumeEventBus() {
    }

}
