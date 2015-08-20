package com.skcraft.plume.common.service.auth;

import com.google.common.collect.Maps;
import lombok.Getter;

import java.util.EnumSet;
import java.util.Map;

/**
 * One of the levels that a permission can be assigned, whether later entries
 * in the enum have greater priority and override lower-priority levels.
 */
public enum Grant {

    /**
     * Access is neither granted or revoked.
     */
    NONE(false, '~'),
    /**
     * Access is granted.
     */
    ALLOW(true, '+'),
    /**
     * Access is denied.
     */
    DENY(false, '-'),
    /**
     * Access
     */
    NEVER(false, '!');

    private static final Map<Character, Grant> grantPrefixes = Maps.newHashMap();

    static {
        for (Grant grant : EnumSet.allOf(Grant.class)) {
            grantPrefixes.put(grant.getPrefix(), grant);
        }
    }

    @Getter private final boolean permit;
    @Getter private final char prefix;

    Grant(boolean permit, char prefix) {
        this.permit = permit;
        this.prefix = prefix;
    }

    public Grant add(Grant grant) {
        return grant.ordinal() > ordinal() ? grant : this;
    }

    public boolean isFinal() {
        return this == NEVER;
    }

    public static Grant baseline() {
        return NONE;
    }

    public static Grant baselineAllow() {
        return ALLOW;
    }
}
