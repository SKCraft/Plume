package com.skcraft.plume.asm.util;

import com.google.gson.annotations.SerializedName;
import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.launchwrapper.Launch;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MethodMatcher {

    @SerializedName("class")
    private String className;
    private String name;
    private String devName;
    private String desc;

    public boolean matchesClass(String testOwner) {
        if (!(Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) {
            FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
            testOwner = remapper.map(testOwner.replace(".", "/")).replace("/", ".");
        }
        return className.equals(testOwner);
    }

    public boolean matchesMethod(String testOwner, String testName, String testDesc) {
        testOwner = testOwner.replace("/", ".");
        if ((Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) {
            return testOwner.equals(className) && testName.equals(devName) && testDesc.equals(desc);
        } else {
            FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
            String srgName = remapper.mapMethodName(testOwner, testName, testDesc);
            String srgDesc = remapper.mapMethodDesc(testDesc);
            return srgName.equals(name) && srgDesc.equals(desc);
        }
    }

    public boolean matchesRemappedMethod(String testOwner, String testName) {
        testOwner = testOwner.replace("/", ".");
        if ((Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) {
            return testOwner.equals(className) && testName.equals(devName);
        } else {
            return testOwner.equals(className) && testName.equals(name);
        }
    }

}
