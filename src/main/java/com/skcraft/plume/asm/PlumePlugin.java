package com.skcraft.plume.asm;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

public class PlumePlugin implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[] {
                "com.skcraft.plume.asm.transformer.PlayerAuthenticateTransformer",
                "com.skcraft.plume.asm.transformer.TickCallbackTransformer",
                "com.skcraft.plume.asm.transformer.ChunkLoadTransformer"
        };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
