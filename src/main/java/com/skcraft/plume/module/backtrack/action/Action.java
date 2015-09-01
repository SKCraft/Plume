package com.skcraft.plume.module.backtrack.action;

import com.google.common.collect.ImmutableList;
import com.skcraft.plume.common.service.journal.Record;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IChatComponent;

import java.util.List;

public interface Action {

    void writeToTag(NBTTagCompound tag);

    void readFromTag(NBTTagCompound tag);

    void undo(Record record);

    void redo(Record record);

    IChatComponent toQueryMessage(Record record);

    default List<ItemStack> copyItems() {
        return ImmutableList.of();
    }

    default void addDetailMessages(Record record, List<IChatComponent> messages) {
        messages.add(toQueryMessage(record));
    }

}
