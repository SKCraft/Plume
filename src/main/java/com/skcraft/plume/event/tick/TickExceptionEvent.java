package com.skcraft.plume.event.tick;

import lombok.Getter;

public class TickExceptionEvent {

    @Getter private final Throwable throwable;

    public TickExceptionEvent(Throwable throwable) {
        this.throwable = throwable;
    }

}
