package com.skcraft.plume.common.util.event;

class RegisteredHandler implements Comparable<RegisteredHandler> {

    private final Handler handler;
    private final Order order;

    RegisteredHandler(Handler handler, Order order) {
        this.handler = handler;
        this.order = order;
    }

    public Handler getHandler() {
        return this.handler;
    }

    public Order getOrder() {
        return this.order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegisteredHandler that = (RegisteredHandler) o;
        return this.handler.equals(that.handler);
    }

    @Override
    public int hashCode() {
        return this.handler.hashCode();
    }

    @Override
    public int compareTo(RegisteredHandler o) {
        return getOrder().ordinal() - o.getOrder().ordinal();
    }

    static RegisteredHandler createForComparison(Handler handler) {
        return new RegisteredHandler(handler, null);
    }

}
