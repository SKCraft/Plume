package com.skcraft.plume.common.util.event;

public interface EventListener<T> {

    public void invoke(T event);

}
