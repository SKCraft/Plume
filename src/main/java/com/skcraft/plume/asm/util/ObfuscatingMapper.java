package com.skcraft.plume.asm.util;

import com.google.common.collect.Maps;
import com.skcraft.plume.util.ReflectionUtils;
import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import lombok.extern.java.Log;
import org.objectweb.asm.commons.Remapper;

import java.util.Map;
import java.util.logging.Level;

@Log
public class ObfuscatingMapper extends Remapper {

    public static final ObfuscatingMapper INSTANCE = new ObfuscatingMapper();

    private final Map<String, String> fieldSrgToObf = Maps.newHashMap();
    private final Map<String, String> methodSrgToObf = Maps.newHashMap();

    ObfuscatingMapper() {
        try {
            Map<String, Map<String, String>> rawFieldMaps = ReflectionUtils.getDeclaredField(FMLDeobfuscatingRemapper.INSTANCE, "rawFieldMaps");
            for (Map.Entry<String, Map<String, String>> entry : rawFieldMaps.entrySet()) {
                for (Map.Entry<String, String> subentry : entry.getValue().entrySet()) {
                    if (subentry.getValue().startsWith("field_")) {
                        fieldSrgToObf.put(subentry.getValue(), subentry.getKey().substring(0, subentry.getKey().indexOf(':')));
                    }
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.log(Level.WARNING, "Failed to get field maps from FMLDeobfuscatingRemapper");
        }

        try {
            Map<String, Map<String, String>> rawMethodMaps = ReflectionUtils.getDeclaredField(FMLDeobfuscatingRemapper.INSTANCE, "rawMethodMaps");
            for (Map.Entry<String, Map<String, String>> entry : rawMethodMaps.entrySet()) {
                for (Map.Entry<String, String> subentry : entry.getValue().entrySet()) {
                    if (subentry.getValue().startsWith("func_")) {
                        methodSrgToObf.put(subentry.getValue(), subentry.getKey().substring(0, subentry.getKey().indexOf('(')));
                    }
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.log(Level.WARNING, "Failed to get method maps from FMLDeobfuscatingRemapper");
        }
    }

    public String obfSrgField(String name) {
        String obf = fieldSrgToObf.get(name);
        return obf != null ? obf : name;
    }

    public String obfSrgMethod(String name) {
        String obf = methodSrgToObf.get(name);
        return obf != null ? obf : name;
    }

    @Override
    public String map(String typeName) {
        return FMLDeobfuscatingRemapper.INSTANCE.unmap(typeName);
    }

    public String mapMethodName(String owner, String name, String desc) {
        return obfSrgMethod(name);
    }

    public String mapInvokeDynamicMethodName(String name, String desc) {
        return obfSrgMethod(name);
    }

    public String mapFieldName(String owner, String name, String desc) {
        return obfSrgField(name);
    }
}
