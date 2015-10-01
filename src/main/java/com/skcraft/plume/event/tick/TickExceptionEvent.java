package com.skcraft.plume.event.tick;

import com.skcraft.plume.event.CrashEvent;

public class TickExceptionEvent extends CrashEvent {

    public TickExceptionEvent(Throwable throwable) {
        super(throwable);
    }
}
