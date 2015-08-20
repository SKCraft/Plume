package com.skcraft.plume.common.auth;

import com.google.common.collect.Lists;

import java.util.List;

public final class LegacyContext {

    private LegacyContext() {
    }

    public static List<String> getEffectivePermissions(String permission, Context context) {
        List<String> permissions = Lists.newArrayList();
        String server = context.get("server");
        String world = context.get("world");
        if (server != null) permissions.add("server." + server + "." + permission);
        if (world != null) permissions.add("world." + world + "." + permission);
        if (server != null && world != null) permissions.add("server." + server + ".world." + world + "." + permission);
        return permissions;
    }

}
