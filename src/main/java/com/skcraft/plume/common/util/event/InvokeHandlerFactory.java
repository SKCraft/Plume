package com.skcraft.plume.common.util.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class InvokeHandlerFactory implements HandlerFactory {

    @Override
    public Handler createHandler(Object object, Method method, boolean ignoreCancelled) {
        return new InvokeHandler(object, method, ignoreCancelled);
    }

    private static class InvokeHandler implements Handler {
        private final Object object;
        private final Method method;
        private final boolean ignoreCancelled;

        public InvokeHandler(Object object, Method method, boolean ignoreCancelled) {
            this.object = object;
            this.method = method;
            this.ignoreCancelled = ignoreCancelled;
        }

        @Override
        public void handle(Object event) throws InvocationTargetException {
            try {
                if (this.ignoreCancelled && (event instanceof Cancellable) && ((Cancellable) event).isCancelled()) {
                    return;
                }
                this.method.invoke(this.object, event);
            } catch (IllegalAccessException e) {
                throw new InvocationTargetException(e);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            InvokeHandler that = (InvokeHandler) o;

            if (!this.method.equals(that.method)) return false;
            if (!this.object.equals(that.object)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = this.object.hashCode();
            result = 31 * result + this.method.hashCode();
            return result;
        }
    }

}
