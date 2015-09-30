package com.skcraft.plume.util.config;

import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import javax.annotation.Nullable;
import java.util.List;

@ConfigSerializable
public class ClassPattern implements Predicate<Class<?>> {

    private static final LoadingCache<String, Class<?>> CLASS_CACHE = CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .weakValues()
            .build(new CacheLoader<String, Class<?>>() {
                @Override
                public Class<?> load(@Nullable String key) throws Exception {
                    try {
                        return Class.forName(key);
                    } catch (ClassNotFoundException e) {
                        return void.class;
                    }
                }
            });

    @Setting(comment = "Class names to match")
    private List<String> instanceOf = Lists.newArrayList();

    @Setting(comment = "Class names to not match")
    private List<String> notInstanceOf = Lists.newArrayList();

    @Setting(comment = "The chance (0 to 1) of ticking, where 1 is 100% or every time (<= 0 to not tick at all)")
    private double chance = 1;

    @Override
    public boolean apply(Class<?> input) {
        if (instanceOf == null) return false;

        boolean matches = false;

        for (String className : instanceOf) {
            Class<?> clazz = CLASS_CACHE.getUnchecked(className);
            if (clazz != void.class) {
                if (clazz.isAssignableFrom(input)) {
                    matches = true;
                } else {
                    return false;
                }
            }
        }

        if (!matches) {
            return false;
        }

        if (notInstanceOf != null) {
            for (String className : notInstanceOf) {
                Class<?> clazz = CLASS_CACHE.getUnchecked(className);
                if (clazz != void.class) {
                    if (clazz.isAssignableFrom(input)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

}
