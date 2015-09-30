package com.skcraft.plume.common.util.event;

public interface EventBus {

    void register(Object obj);

    void unregister(Object obj);

    default boolean post(Object event) {
        return post(event, true);
    }

    boolean post(Object event, boolean suppressExceptions);

}
