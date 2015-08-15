package com.skcraft.plume.event;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This event is an internal event. We do not recommend handling or throwing
 * this event or its subclasses as the interface is highly subject to change.
 */
public abstract class DelegateEvent implements Cancellable {

    private final Cause cause;
    private Result result = Result.DEFAULT;

    /**
     * Create a new instance
     *
     * @param cause The cause
     */
    protected DelegateEvent(Cause cause) {
        checkNotNull(cause);
        this.cause = cause;
    }

    /**
     * Return the cause.
     *
     * @return the cause
     */
    public Cause getCause() {
        return cause;
    }

    @Override
    public boolean isCancelled() {
        return getResult() == Result.DENY;
    }

    @Override
    public void setCancelled(boolean cancel) {
        if (cancel) {
            setResult(Result.DENY);
        }
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    protected <A, B> boolean filter(List<A> list, Function<A, B> function, Predicate<B> predicate, boolean cancelEventOnFalse) {
        boolean hasRemoval = false;
        cancelEventOnFalse = true; // We don't support advanced list modification yet

        Iterator<B> it = Iterators.transform(list.iterator(), function);
        while (it.hasNext()) {
            if (!predicate.apply(it.next())) {
                hasRemoval = true;

                if (cancelEventOnFalse) {
                    list.clear();
                    setCancelled(true);
                    break;
                } else {
                    it.remove();
                }
            }
        }

        return hasRemoval;
    }

}
