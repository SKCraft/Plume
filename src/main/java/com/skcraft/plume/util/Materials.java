package com.skcraft.plume.util;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

public final class Materials {

    private Materials() {
    }

    public static boolean isWater(Block block) {
        return block == Blocks.water || block == Blocks.flowing_water;
    }

}
