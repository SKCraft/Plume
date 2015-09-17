package com.skcraft.plume.util.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import static com.google.common.base.Preconditions.checkNotNull;

public class PlayerInventoryChestAdapter implements IInventory {

    private InventoryPlayer delegate;
    private final String inventoryName;

    public PlayerInventoryChestAdapter(InventoryPlayer delegate, String inventoryName) {
        checkNotNull(delegate, "delegate");
        this.delegate = delegate;
        this.inventoryName = inventoryName;
    }

    public InventoryPlayer getDelegate() {
        return delegate;
    }

    protected void setDelegate(InventoryPlayer delegate) {
        checkNotNull(delegate, "delegate");
        this.delegate = delegate;
    }

    @Override
    public int getSizeInventory() {
        return 54;
    }

    private int getActualSize() {
        return delegate.mainInventory.length + delegate.armorInventory.length;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        if (index >= getActualSize()) {
            return null;
        }

        return delegate.getStackInSlot(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int amount) {
        if (index >= getActualSize()) {
            return null;
        }

        ItemStack ret = delegate.decrStackSize(index, amount);
        delegate.markDirty();
        return ret;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        if (index >= getActualSize()) {
            return null;
        }

        return delegate.getStackInSlotOnClosing(index);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack item) {
        if (index >= getActualSize()) {
            return;
        }

        delegate.setInventorySlotContents(index, item);
        delegate.markDirty();
    }

    @Override
    public String getInventoryName() {
        return inventoryName;
    }

    @Override
    public boolean hasCustomInventoryName() {
        return true;
    }

    @Override
    public int getInventoryStackLimit() {
        return delegate.getInventoryStackLimit();
    }

    @Override
    public void markDirty() {
        delegate.markDirty();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack item) {
        return index < getActualSize() && delegate.isItemValidForSlot(index, item);

    }

}
