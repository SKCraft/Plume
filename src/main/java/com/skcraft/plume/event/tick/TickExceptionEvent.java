package com.skcraft.plume.event.tick;

import cpw.mods.fml.common.eventhandler.Event;
import lombok.Getter;

public class TickExceptionEvent extends Event {

    @Getter private final Throwable throwable;

    public TickExceptionEvent(Throwable throwable) {
        this.throwable = throwable;
    }

}
