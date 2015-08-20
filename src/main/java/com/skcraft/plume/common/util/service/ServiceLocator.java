package com.skcraft.plume.common.util.service;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;

import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class ServiceLocator {

    private final ConcurrentMap<Class<?>, Object> providers = Maps.newConcurrentMap();

    @SuppressWarnings("unchecked")
    public <T> void register(Class<?> service, T implementation) {
        checkNotNull(service, "service");
        checkNotNull(implementation, "implementation");
        T previous;
        if ((previous = (T) providers.putIfAbsent(service, implementation)) != null) {
            throw new IllegalArgumentException("There is already an implementation (" + previous.getClass().getName() + ") for " + service.getName());
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T provide(Class<T> service) throws NoProviderExistsException {
        T provider = (T) providers.get(service);
        if (provider != null) {
            return provider;
        } else {
            throw new NoProviderExistsException("No provider is known for " + service);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(Class<T> service) {
        T provider = (T) providers.get(service);
        if (provider != null) {
            return Optional.fromNullable(provider);
        } else {
            return Optional.absent();
        }
    }

}
