package com.skcraft.plume.common.util.event;

import java.lang.reflect.InvocationTargetException;

public interface Handler {

    void handle(Object event) throws InvocationTargetException;

}
