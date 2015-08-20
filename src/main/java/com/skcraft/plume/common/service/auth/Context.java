package com.skcraft.plume.common.service.auth;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Context {

    private final Map<String, String> values;

    private Context(Map<String, String> values) {
        this.values = values;
    }

    public String get(String key) {
        checkNotNull(key, "key");
        return values.get(key);
    }

    public static class Builder {
        private final Map<String, String> values = Maps.newHashMap();

        public String get(String key) {
            checkNotNull(key, "key");
            return values.get(key);
        }

        public Builder put(String key, String value) {
            checkNotNull(key, "key");
            checkNotNull(value, "value");
            values.put(key, value);
            return this;
        }

        public Builder remove(String key) {
            checkNotNull(key, "key");
            values.remove(key);
            return this;
        }

        public void clear() {
            values.clear();
        }

        public Context build() {
            return new Context(ImmutableMap.copyOf(values));
        }
    }

}
