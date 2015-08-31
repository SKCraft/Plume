package com.skcraft.plume.module.backtrack.action;

import com.skcraft.plume.util.NamedEntity;
import lombok.Data;
import net.minecraft.nbt.NBTTagCompound;

@Data
abstract class NamedEntityAction implements Action {

    private NamedEntity entity;

    @Override
    public void writeToTag(NBTTagCompound tag) {
        NBTTagCompound entityTag = new NBTTagCompound();
        entity.writeToTag(entityTag);
        tag.setTag("NamedEntity", entityTag);
    }

    @Override
    public void readFromTag(NBTTagCompound tag) {
        entity = NamedEntity.readFromTag(tag.getCompoundTag("NamedEntity"));
    }

}
