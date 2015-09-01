package com.skcraft.plume.module.backtrack.action;

import com.skcraft.plume.common.service.journal.Record;
import lombok.Data;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

@Data
public class PlayerChatAction implements Action {

    private String message;

    @Override
    public void writeToTag(NBTTagCompound tag) {
        tag.setString("Message", message);
    }

    @Override
    public void readFromTag(NBTTagCompound tag) {
        message = tag.getString("Message");
    }

    @Override
    public void undo(Record record) {
    }

    @Override
    public void redo(Record record) {
    }

    @Override
    public IChatComponent toQueryMessage(Record record) {
        return new ChatComponentText(String.format("%s said at %d:%d:%d: %s",
                record.getUserId().getName(),
                record.getLocation().getX(),
                record.getLocation().getY(),
                record.getLocation().getZ(),
                getMessage()));
    }

}
