package com.skcraft.plume.module.backtrack.action;

import com.skcraft.plume.common.service.journal.Record;
import com.skcraft.plume.util.NBTConstants;
import com.skcraft.plume.util.NBTUtils;
import lombok.Data;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class PlayerDeathAction implements Action {

    private List<ItemStack> itemStacks;

    @Override
    public void writeToTag(NBTTagCompound tag) {
        tag.setTag("Items", NBTUtils.itemStacksToNBT(itemStacks));
    }

    @Override
    public void readFromTag(NBTTagCompound tag) {
        itemStacks = NBTUtils.nbtToItemStacks(tag.getTagList("Items", NBTConstants.COMPOUND_TAG));
    }

    @Override
    public void undo(Record record) {
    }

    @Override
    public void redo(Record record) {
    }

    @Override
    public IChatComponent toQueryMessage(Record record) {
        return new ChatComponentText(String.format("%s died at %d:%d:%d",
                record.getUserId().getName(),
                record.getLocation().getX(),
                record.getLocation().getY(),
                record.getLocation().getZ()));
    }

    @Override
    public void addDetailMessages(Record record, List<IChatComponent> messages) {
        Action.super.addDetailMessages(record, messages);
        messages.add(new ChatComponentText("Items: " + getItemStacks()));
    }

    @Override
    public List<ItemStack> copyItems() {
        return itemStacks.stream().map(ItemStack::copy).collect(Collectors.toList());
    }

}
