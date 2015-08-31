package com.skcraft.plume.util;

import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.util.WorldVector3i;
import lombok.Data;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.List;

@Data
public class TrackablePlayerSnapshot {

    private final UserId userId;
    private final WorldVector3i location;
    private final List<ItemStack> items;

    public void writeToTag(NBTTagCompound tag) {
        tag.setTag("Player", NBTUtils.userIdToNBT(userId));
        NBTUtils.writeWorldVector3iToNBT(location, tag);
        tag.setTag("Items", NBTUtils.itemStacksToNBT(items));
    }

    public static TrackablePlayerSnapshot readFromTag(NBTTagCompound tag) {
        return new TrackablePlayerSnapshot(
                NBTUtils.nbtToUserId(tag.getCompoundTag("Player")),
                NBTUtils.readWorldVector3iFromNBT(tag),
                NBTUtils.nbtToItemStacks(tag.getTagList("Items", NBTConstants.COMPOUND_TAG)));
    }

}
