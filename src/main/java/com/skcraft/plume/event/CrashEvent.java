package com.skcraft.plume.event;

import com.skcraft.plume.common.util.event.Cancellable;
import lombok.Getter;

public class CrashEvent implements Cancellable {

    @Getter
    private final Throwable throwable;
    private boolean cancelled;

    public CrashEvent(Throwable throwable) {
        this.throwable = throwable;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
