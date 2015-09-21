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
        return className.equals(testOwner);
    }

    public boolean matchesMethod(String testOwner, String testName, String testDesc) {
        testOwner = testOwner.replace("/", ".");
        if ((Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) {
            return testOwner.equals(className) && testName.equals(devName) && testDesc.equals(desc);
        } else {
            FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
            String srgName = remapper.mapMethodName(remapper.unmap(testOwner.replace(".", "/")), testName, testDesc);
            String srgDesc = remapper.mapMethodDesc(testDesc);
            return testOwner.equals(className) && srgName.equals(name) && srgDesc.equals(desc);
        }
    }

}
