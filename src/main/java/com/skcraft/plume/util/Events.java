package com.skcraft.plume.util;

import com.sk89q.worldedit.util.eventbus.EventBus;
import com.skcraft.plume.event.DelegateEvent;
import cpw.mods.fml.common.eventhandler.Event;

/**
 * Some event utility functions.
 */
public final class Events {

    private Events() {
    }

    /**
     * Set eventToFire's cancellation status to the original event,
     * fire eventToFire, and then if eventToFire was cancelled,
     * fire the original event.
     *
     * @param eventBus The event bus
     * @param eventToFire The event to fire to consider
     * @param original The original event to potentially cancel
     * @return Whether the event was fired and it caused the original event to be cancelled
     */
    public static boolean postDelegate(EventBus eventBus, DelegateEvent eventToFire, Event original) {
        if (original.isCanceled()) {
            eventToFire.isCancelled();
        }

        eventBus.post(eventToFire);

        if (!original.isCanceled() && eventToFire.isCancelled()) {
            original.setCanceled(true);
            return true;
        }

        return false;
    }

}
