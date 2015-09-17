package com.skcraft.plume.util.inventory;

import com.google.common.collect.Lists;
import com.skcraft.plume.util.Items;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

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

    public static List<ItemStack> copyInventoryItems(InventoryPlayer inventory) {
        List<ItemStack> items = Lists.newArrayList();

        for (int i = 0; i < inventory.mainInventory.length; ++i) {
            if (inventory.mainInventory[i] != null) {
                items.add(inventory.mainInventory[i].copy());
            }
        }

        for (int i = 0; i < inventory.armorInventory.length; ++i) {
            if (inventory.armorInventory[i] != null) {
                items.add(inventory.armorInventory[i].copy());
            }
        }

        return items;
    }

    public static void copyDirectly(IInventory from, IInventory to) {
        for (int i = 0; i < from.getSizeInventory(); i++) {
            ItemStack fromItem = from.getStackInSlot(i);
            to.setInventorySlotContents(i, fromItem != null ? fromItem.copy() : null);
        }
    }

    public static void openVirtualInventory(String name, EntityPlayerMP player, List<ItemStack> items) {
        checkArgument(items.size() <= 54, "items.size() <= 54");
        InventoryBasic inventory = new InventoryBasic(name, true, 54);
        for (int i = 0; i < items.size(); i++) {
            inventory.setInventorySlotContents(i, items.get(i));
        }
        player.displayGUIChest(inventory);
    }

}
