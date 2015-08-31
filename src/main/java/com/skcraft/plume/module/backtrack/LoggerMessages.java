package com.skcraft.plume.module.backtrack;

import com.skcraft.plume.util.BlockSnapshot;
import com.skcraft.plume.util.GameRegistryUtils;
import net.minecraft.block.Block;

public final class LoggerMessages {

    private LoggerMessages() {
    }

    public static String getBlockName(BlockSnapshot snapshot) {
        Block block = snapshot.getBlock();
        String name = GameRegistryUtils.getBlockId(block);
        if (name != null) {
            return name.replaceAll("^[^:]+:", "").replace("_", ""); // Watson doesn't support underscores
        } else {
            return block.getUnlocalizedName();
        }
    }

}
