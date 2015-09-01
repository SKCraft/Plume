package com.skcraft.plume.util;

import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

import javax.annotation.Nullable;

public final class GameRegistryUtils {

    private GameRegistryUtils() {
    }

    @Nullable
    public static String getBlockId(Block block) {
        return GameData.getBlockRegistry().getNameForObject(block);
    }

    @Nullable
    public static Block fromBlockId(String name) {
        return GameData.getBlockRegistry().getObject(name);
    }

    public static Block getDefaultBlock() {
        return GameData.getBlockRegistry().getDefaultValue();
    }

    @Nullable
    public static String getItemId(Item item) {
        return GameData.getItemRegistry().getNameForObject(item);
    }

}
