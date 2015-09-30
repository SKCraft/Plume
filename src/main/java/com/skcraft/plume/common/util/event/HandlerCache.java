package com.skcraft.plume.common.util.event;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

class HandlerCache {

    private final List<Handler> handlers;

    @SuppressWarnings("unchecked")
    HandlerCache(List<RegisteredHandler> registrations) {
        this.handlers = Lists.newArrayList();
        for (RegisteredHandler reg : registrations) {
            this.handlers.add(reg.getHandler());
        }
    }

    public List<Handler> getHandlers() {
        return this.handlers;
    }

}
