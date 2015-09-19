package com.skcraft.plume.util.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class Items {

    private Items() {
    }

    public static NBTTagCompound getOrCreateTagCompound(ItemStack item) {
        NBTTagCompound compound = item.getTagCompound();
        if (compound == null) {
            compound = new NBTTagCompound();
            item.setTagCompound(compound);
        }
        return compound;
    }

}
