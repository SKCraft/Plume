package com.skcraft.plume.common.util.event;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import lombok.extern.java.Log;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkNotNull;

@Log
public class SimpleEventBus implements EventBus {

    private final Object lock = new Object();
    private final HandlerFactory handlerFactory = new HandlerClassFactory("plumehandler");
    private final Multimap<Class<?>, RegisteredHandler> handlersByEvent = HashMultimap.create();

    /**
     * A cache of all the handlers for an event type for quick event posting.
     *
     * <p>The cache is currently entirely invalidated if handlers are added
     * or removed.</p>
     */
    private final LoadingCache<Class<?>, HandlerCache> handlersCache =
            CacheBuilder.newBuilder().build(new CacheLoader<Class<?>, HandlerCache>() {
                @Override
                public HandlerCache load(Class<?> type) throws Exception {
                    return bakeHandlers(type);
                }
            });

    @SuppressWarnings("unchecked")
    private HandlerCache bakeHandlers(Class<?> rootType) {
        List<RegisteredHandler> registrations = Lists.newArrayList();
        Set<Class<?>> types = (Set) TypeToken.of(rootType).getTypes().rawTypes();

        synchronized (this.lock) {
            for (Class<?> type : types) {
                registrations.addAll(this.handlersByEvent.get(type));
            }
        }

        Collections.sort(registrations);

        return new HandlerCache(registrations);
    }

    private HandlerCache getHandlerCache(Class<?> type) {
        return this.handlersCache.getUnchecked(type);
    }

    @SuppressWarnings("unchecked")
    private List<Subscriber> findAllSubscribers(Object object) {
        List<Subscriber> subscribers = Lists.newArrayList();
        Class<?> type = object.getClass();

        for (Method method : type.getMethods()) {
            @Nullable Subscribe subscribe = method.getAnnotation(Subscribe.class);

            if (subscribe != null) {
                Class<?>[] paramTypes = method.getParameterTypes();

                if (isValidHandler(method)) {
                    Class<?> eventClass = paramTypes[0];
                    Handler handler = this.handlerFactory.createHandler(object, method, subscribe.ignoreCancelled());
                    subscribers.add(new Subscriber(eventClass, handler, subscribe.order()));
                } else {
                    log.log(Level.WARNING, "The method {0} on {1} has @{2} but has the wrong signature",
                            new Object[] { method, method.getDeclaringClass().getName(), Subscribe.class.getName() });
                }
            }
        }

        return subscribers;
    }

    public boolean register(Class<?> type, Handler handler, Order order) {
        return register(new Subscriber(type, handler, order));
    }

    public boolean register(Subscriber subscriber) {
        return registerAll(Lists.newArrayList(subscriber));
    }

    private boolean registerAll(List<Subscriber> subscribers) {
        synchronized (this.lock) {
            boolean changed = false;

            for (Subscriber sub : subscribers) {
                if (this.handlersByEvent.put(sub.getEventClass(), new RegisteredHandler(sub.getHandler(), sub.getOrder()))) {
                    changed = true;
                }
            }

            if (changed) {
                this.handlersCache.invalidateAll();
            }

            return changed;
        }
    }

    public boolean unregister(Class<?> type, Handler handler) {
        return unregister(new Subscriber(type, handler));
    }

    public boolean unregister(Subscriber subscriber) {
        return unregisterAll(Lists.newArrayList(subscriber));
    }

    public boolean unregisterAll(List<Subscriber> subscribers) {
        synchronized (this.lock) {
            boolean changed = false;

            for (Subscriber sub : subscribers) {
                if (this.handlersByEvent.remove(sub.getEventClass(), RegisteredHandler.createForComparison(sub.getHandler()))) {
                    changed = true;
                }
            }

            if (changed) {
                this.handlersCache.invalidateAll();
            }

            return changed;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void register(Object object) {
        checkNotNull(object, "object");
        registerAll(findAllSubscribers(object));
    }

    @Override
    public void unregister(Object object) {
        checkNotNull(object, "object");
        unregisterAll(findAllSubscribers(object));
    }

    @Override
    public boolean post(Object event, boolean suppressExceptions) {
        checkNotNull(event, "event");

        for (Handler handler : getHandlerCache(event.getClass()).getHandlers()) {
            try {
                handler.handle(event);
            } catch (Exception t) {
                if (!suppressExceptions) {
                    throw new EventPostException(t);
                }
                log.log(Level.WARNING, "A handler raised an error when handling an event", t);
            }
        }

        return event instanceof Cancellable && ((Cancellable) event).isCancelled();
    }

    private static boolean isValidHandler(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        return !Modifier.isStatic(method.getModifiers())
                && !Modifier.isAbstract(method.getModifiers())
                && !Modifier.isInterface(method.getDeclaringClass().getModifiers())
                && method.getReturnType() == void.class
                && paramTypes.length == 1;
    }

}
