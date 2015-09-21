package com.skcraft.plume.asm.util;

import net.minecraft.launchwrapper.Launch;

import java.util.HashMap;
import java.util.Map;

public final class ObfMappings {

    private static final Map<String, String> mappings = new HashMap<>();
    private static final boolean devEnvironment = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");

    public static String get(String key) {
        return devEnvironment ? key : mappings.get(key);
    }

    static {
        mappings.put("net/minecraft/server/network/NetHandlerLoginServer", "nn");
        mappings.put("net/minecraft/server/network/NetHandlerLoginServer$1", "no");
        mappings.put("net/minecraft/network/NetHandlerPlayServer", "nh");
        mappings.put("uncheckedTryHarvestBlock", "a");
    }
}
