package com.skcraft.plume.common.util.module;

import com.google.inject.*;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.ConfigFactory;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.config.DataDir;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;

class PlumeModule extends AbstractModule {

    private final File dataDir;
    private final EventBus eventBus = new EventBus();
    private final ConfigFactory configFactory;

    PlumeModule(File dataDir) {
        this.dataDir = dataDir;
        this.configFactory = new ConfigFactory(dataDir);
    }

    @Override
    protected void configure() {
        bindScope(Module.class, Scopes.SINGLETON);
        bindScope(AutoRegister.class, Scopes.SINGLETON);

        bind(File.class).annotatedWith(DataDir.class).toInstance(dataDir);
        bind(EventBus.class).toInstance(eventBus);
        bind(ModuleLoader.class).in(Singleton.class);

        // Event bus registration
        bindListener(Matchers.any(), new TypeListener() {
            @Override
            public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
                Class<? super I> rawType = type.getRawType();
                if (rawType.isAnnotationPresent(AutoRegister.class) || rawType.isAnnotationPresent(Module.class)) {
                    encounter.register((InjectionListener<I>) eventBus::register);
                }
            }
        });

        // Handle config / service injection
        bindListener(Matchers.any(), new TypeListener() {
            @Override
            public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
                Class<?> clazz = type.getRawType();
                while (clazz != null) {
                    for (Field field : clazz.getDeclaredFields()) {
                        if (field.getType() == Config.class && field.isAnnotationPresent(InjectConfig.class)) {
                            InjectConfig annotation = field.getAnnotation(InjectConfig.class);

                            encounter.register((MembersInjector<I>) instance -> {
                                try {
                                    field.setAccessible(true);
                                    ParameterizedType paramType = (ParameterizedType) field.getGenericType();
                                    field.set(instance, configFactory.createMapping(annotation.value(), (Class<?>) paramType.getActualTypeArguments()[0]));
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException("Failed to set @InjectConfig", e);
                                }
                            });
                        }
                    }
                    clazz = clazz.getSuperclass();
                }
            }
        });
    }

}
