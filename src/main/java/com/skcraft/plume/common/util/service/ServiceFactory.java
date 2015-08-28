package com.skcraft.plume.common.util.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.sk89q.worldedit.util.eventbus.EventHandler.Priority;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.skcraft.plume.common.event.lifecycle.InitializationVerifyEvent;
import com.skcraft.plume.common.util.FatalError;
import com.skcraft.plume.common.util.module.AutoRegister;
import com.skcraft.plume.common.util.module.Modules;

import java.util.Collection;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

@AutoRegister
public class ServiceFactory {

    private final ServiceLocator locator;
    private final Multimap<Class<?>, Class<?>> required = ArrayListMultimap.create();
    private boolean loaded = false;

    @Inject
    public ServiceFactory(ServiceLocator locator) {
        checkNotNull(locator, "locator");
        this.locator = locator;
    }

    @SuppressWarnings("unchecked")
    public <T> Service<T> create(Class<?> service, Class<?> requester, boolean required) {
        synchronized (this) {
            if (required && !locator.get(service).isPresent()) {
                if (!loaded) {
                    this.required.put(service, requester);
                } else {
                    throw new NoProviderExistsException(createMissingProviderMessage(service, Lists.newArrayList(requester)));
                }
            }
        }
        return new Service<>(locator, (Class<T>) service);
    }

    @Subscribe(priority = Priority.VERY_EARLY)
    public void onInitializationVerify(InitializationVerifyEvent event) {
        synchronized (this) {
            if (!loaded) {
                loaded = true;
                for (Map.Entry<Class<?>, Collection<Class<?>>> entry : required.asMap().entrySet()) {
                    if (!locator.get(entry.getKey()).isPresent()) {
                        event.getFatalErrors().add(new FatalError(createMissingProviderMessage(entry.getKey(), entry.getValue())));
                    }
                }
                required.clear();
            }
        }
    }

    private String createMissingProviderMessage(Class<?> service, Collection<Class<?>> requesters) {
        StringBuilder builder = new StringBuilder();
        builder.append("The service '").append(service.getName()).append("' is required by:");
        for (Class<?> requester : requesters) {
            builder.append("\n\t").append(Modules.getModuleName(requester));
        }
        return builder.toString();
    }

}
