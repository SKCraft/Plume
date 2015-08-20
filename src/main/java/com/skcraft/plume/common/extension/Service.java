package com.skcraft.plume.common.extension;

import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class Service<T> {

    private final ServiceLocator locator;
    private final Class<T> service;

    public Service(ServiceLocator locator, Class<T> service) {
        checkNotNull(locator, "locator");
        checkNotNull(service, "service");
        this.locator = locator;
        this.service = service;
    }

    public Optional<T> get() {
        return locator.get(service);
    }

    public T provide() {
        return locator.provide(service);
    }

}
