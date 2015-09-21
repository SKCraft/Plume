package com.skcraft.plume.asm.util;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraft.launchwrapper.Launch;

public final class ObfHelper {

    private static final boolean DEV_ENVIRONMENT = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");

    private ObfHelper() {
    }

    public static String obfMethodDesc(String desc) {
        return FMLDeobfuscatingRemapper.INSTANCE.mapMethodDesc(desc);
    }

}
