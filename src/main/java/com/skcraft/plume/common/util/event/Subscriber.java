package com.skcraft.plume.common.util.event;

import static com.google.common.base.Preconditions.checkNotNull;

class Subscriber {

    private final Class<?> eventClass;
    private final Handler handler;
    private final Order order;

    Subscriber(Class<?> eventClass, Handler handler) {
        this(eventClass, handler, Order.DEFAULT);
    }

    Subscriber(Class<?> eventClass, Handler handler, Order order) {
        checkNotNull(eventClass, "eventClass");
        checkNotNull(handler, "handler");
        checkNotNull(order, "order");
        this.eventClass = eventClass;
        this.handler = handler;
        this.order = order;
    }

    public Class<?> getEventClass() {
        return this.eventClass;
    }

    public Handler getHandler() {
        return this.handler;
    }

    public Order getOrder() {
        return this.order;
    }

}
