package com.skcraft.plume.module.backtrack.action;

import com.skcraft.plume.common.service.journal.Record;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ItemPickupAction extends ItemAction {

    @Override
    public void undo(Record record) {
    }

    @Override
    public void redo(Record record) {
    }

    @Override
    public IChatComponent toQueryMessage(Record record) {
        return new ChatComponentText(String.format("%s picked up %s at %d:%d:%d",
                record.getUserId().getName(),
                getItemStack(),
                record.getLocation().getX(),
                record.getLocation().getY(),
                record.getLocation().getZ()));
    }

}
