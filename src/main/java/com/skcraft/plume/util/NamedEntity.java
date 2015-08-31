package com.skcraft.plume.util;

import lombok.Data;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;

@Data
public class NamedEntity {

    private final String name;

    public void writeToTag(NBTTagCompound target) {
        target.setString("name", name);
    }

    public static NamedEntity readFromTag(NBTTagCompound tag) {
        return new NamedEntity(tag.getString("name"));
    }

    public static NamedEntity from(Entity entity) {
        String name = EntityList.getEntityString(entity);
        if (name == null) {
            name = entity.getClass().getName();
        }
        return new NamedEntity(name);
    }
}
