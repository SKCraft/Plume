package com.skcraft.plume.module.backtrack.action;

import com.skcraft.plume.common.service.journal.Record;
import com.skcraft.plume.module.backtrack.LoggerMessages;
import com.skcraft.plume.util.BlockSnapshot;
import com.skcraft.plume.util.NBTUtils;
import lombok.Data;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import static com.google.common.base.Preconditions.checkNotNull;

@Data
abstract class BlockChangeAction implements Action {

    private BlockSnapshot before;
    private BlockSnapshot after;

    @Override
    public void writeToTag(NBTTagCompound tag) {
        checkNotNull(before, "before");
        checkNotNull(after, "after");

        tag.setTag("Before", NBTUtils.writeToCompound(before::writeToTag));
        tag.setTag("After", NBTUtils.writeToCompound(after::writeToTag));
    }

    @Override
    public void readFromTag(NBTTagCompound tag) {
        before = BlockSnapshot.readFromTag(tag.getCompoundTag("Before"));
        after = BlockSnapshot.readFromTag(tag.getCompoundTag("After"));
    }

    @Override
    public void undo(Record record) {
        before.placeInWorld(record.getLocation(), true);
    }

    @Override
    public void redo(Record record) {
        after.placeInWorld(record.getLocation(), true);
    }

    @Override
    public IChatComponent toQueryMessage(Record record) {
        boolean destroyed = getAfter().getBlock() == Blocks.air;

        // WARNING: This exact wording is needed to have Watson detect
        // the change
        return new ChatComponentText(String.format("%s %s %s at %d:%d:%d",
                record.getUserId().getName(),
                destroyed ? "destroyed" : "created",
                LoggerMessages.getBlockName(destroyed ? getBefore() : getAfter()),
                record.getLocation().getX(),
                record.getLocation().getY(),
                record.getLocation().getZ()));
    }

}
