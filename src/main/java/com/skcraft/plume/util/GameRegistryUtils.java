package com.skcraft.plume.util;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

import javax.annotation.Nullable;

public final class GameRegistryUtils {

    private GameRegistryUtils() {
    }

    @Nullable
    public static String getBlockId(Block block) {
        return GameData.getBlockRegistry().getNameForObject(block).toString();
    }

    @Nullable
    public static Block fromBlockId(String name) {
        return GameData.getBlockRegistry().getObject(new ResourceLocation(name));
    }

    public static Block getDefaultBlock() {
        return GameData.getBlockRegistry().getDefaultValue();
    }

    @Nullable
    public static String getItemId(Item item) {
        return GameData.getItemRegistry().getNameForObject(item).toString();
    }

}
