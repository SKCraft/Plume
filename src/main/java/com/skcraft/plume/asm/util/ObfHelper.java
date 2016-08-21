package com.skcraft.plume.asm.util;

import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraft.launchwrapper.Launch;

public final class ObfHelper {

    private static final boolean DEV_ENVIRONMENT = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");

    private ObfHelper() {
    }

    public static String obfClass(String name) {
        if (DEV_ENVIRONMENT) {
            return name;
        }
        return FMLDeobfuscatingRemapper.INSTANCE.unmap(name);
    }

    public static String obfMethodDesc(String desc) {
        if (DEV_ENVIRONMENT) {
            return desc;
        }
        return ObfuscatingMapper.INSTANCE.mapMethodDesc(desc);
    }

    public static String obfDesc(String desc) {
        if (DEV_ENVIRONMENT) {
            return desc;
        }
        return ObfuscatingMapper.INSTANCE.mapDesc(desc);
    }

    public static String obfMethod(String owner, String name, String devName, String desc) {
        if (DEV_ENVIRONMENT) {
            return devName;
        }
        return ObfuscatingMapper.INSTANCE.mapMethodName(owner, name, desc);
    }

    public static String obfField(String owner, String name, String devName, String desc) {
        if (DEV_ENVIRONMENT) {
            return devName;
        }
        return ObfuscatingMapper.INSTANCE.mapFieldName(owner, name, desc);
    }

}
