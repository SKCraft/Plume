package com.skcraft.plume.common.util.event;

public interface Cancellable {

    boolean isCancelled();

    void setCancelled(boolean cancelled);

}
