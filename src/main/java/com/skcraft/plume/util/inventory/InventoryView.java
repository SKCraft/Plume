package com.skcraft.plume.util.inventory;

import com.google.common.collect.Lists;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class InventoryView implements IInventory {

    private final IInventory inventory;

    public InventoryView(IInventory inventory) {
        this.inventory = inventory;
    }

    public int getCountOf(ItemStackMatcher matcher) {
        checkNotNull(matcher, "matcher");
        int count = 0;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack item = inventory.getStackInSlot(slot);
            if (item != null && matcher.apply(item)) {
                count += item.stackSize;
            }
        }
        return count;
    }

    public List<ItemStack> remove(ItemStackMatcher matcher, int requested) {
        checkNotNull(matcher, "matcher");
        checkArgument(requested > 0, "requested > 0");
        List<ItemStack> removed = Lists.newArrayList();
        int remaining = requested;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack item = inventory.getStackInSlot(slot);
            if (item != null && matcher.apply(item) && item.stackSize > 0) {
                if (item.stackSize <= remaining) {
                    removed.add(item.copy());
                    remaining -= item.stackSize;
                    inventory.setInventorySlotContents(slot, null);
                } else {
                    item.stackSize -= remaining;
                    ItemStack removedItem = item.copy();
                    removedItem.stackSize = remaining;
                    removed.add(removedItem);
                    remaining = 0;
                }

                if (remaining == 0) {
                    break;
                }
            }
        }
        return removed;
    }

    @Override
    public int getSizeInventory() {
        return inventory.getSizeInventory();
    }

    @Override
    public ItemStack getStackInSlot(int p_70301_1_) {
        return inventory.getStackInSlot(p_70301_1_);
    }

    @Override
    public ItemStack decrStackSize(int p_70298_1_, int p_70298_2_) {
        return inventory.decrStackSize(p_70298_1_, p_70298_2_);
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return inventory.removeStackFromSlot(index);
    }

    @Override
    public void setInventorySlotContents(int p_70299_1_, ItemStack p_70299_2_) {
        inventory.setInventorySlotContents(p_70299_1_, p_70299_2_);
    }

    @Override
    public int getInventoryStackLimit() {
        return inventory.getInventoryStackLimit();
    }

    @Override
    public void markDirty() {
        inventory.markDirty();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer p_70300_1_) {
        return inventory.isUseableByPlayer(p_70300_1_);
    }

    @Override
    public void openInventory(EntityPlayer player) {
        inventory.openInventory(player);
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        inventory.closeInventory(player);
    }

    @Override
    public boolean isItemValidForSlot(int p_94041_1_, ItemStack p_94041_2_) {
        return inventory.isItemValidForSlot(p_94041_1_, p_94041_2_);
    }

    @Override
    public int getField(int id) {
        return inventory.getField(id);
    }

    @Override
    public void setField(int id, int value) {
        inventory.setField(id, value);
    }

    @Override
    public int getFieldCount() {
        return inventory.getFieldCount();
    }

    @Override
    public void clear() {
        inventory.clear();
    }

    @Override
    public String getName() {
        return inventory.getName();
    }

    @Override
    public boolean hasCustomName() {
        return inventory.hasCustomName();
    }

    @Override
    public IChatComponent getDisplayName() {
        return inventory.getDisplayName();
    }

}
