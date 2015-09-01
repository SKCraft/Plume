package com.skcraft.plume.module.backtrack.action;

import com.google.common.collect.Lists;
import com.skcraft.plume.util.NBTUtils;
import lombok.Data;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Data
abstract class ItemAction implements Action {

    private ItemStack itemStack;

    @Override
    public void writeToTag(NBTTagCompound tag) {
        checkNotNull(itemStack, "itemStack");
        tag.setTag("Item", NBTUtils.writeToCompound(itemStack::writeToNBT));
    }

    @Override
    public void readFromTag(NBTTagCompound tag) {
        this.itemStack = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Item"));
    }

    @Override
    public List<ItemStack> copyItems() {
        return Lists.newArrayList(itemStack.copy());
    }

}
