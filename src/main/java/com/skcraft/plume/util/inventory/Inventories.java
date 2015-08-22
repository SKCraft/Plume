package com.skcraft.plume.util.inventory;

import com.skcraft.plume.util.Items;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public final class Inventories {

    private Inventories() {
    }

    public static TypeMatcher typeMatcher(ItemStack stack) {
        return new TypeMatcher(stack);
    }

    public static TypeDataMatcher typeDamageMatcher(ItemStack stack) {
        return new TypeDataMatcher(stack);
    }

    public static void giveItem(EntityPlayer player, ItemStack item) {
        if (!player.inventory.addItemStackToInventory(item)) {
            Items.dropItem(player.worldObj, player.posX, player.posY, player.posZ, item);
        }
    }

}
