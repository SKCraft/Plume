package com.skcraft.plume.util;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Items {

    private Items() {
    }

    public static void dropItem(World world, double x, double y, double z, ItemStack itemStack) {
        checkNotNull(world, "world");
        checkNotNull(itemStack, "itemStack");
        EntityItem entity = new EntityItem(world, x, y, z, itemStack);
        entity.delayBeforeCanPickup = 10;
        world.spawnEntityInWorld(entity);
    }

}
