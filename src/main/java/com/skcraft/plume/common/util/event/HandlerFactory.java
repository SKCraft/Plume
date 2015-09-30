package com.skcraft.plume.common.util.event;

import java.lang.reflect.Method;

interface HandlerFactory {

    Handler createHandler(Object object, Method method, boolean ignoreCancelled);

}
