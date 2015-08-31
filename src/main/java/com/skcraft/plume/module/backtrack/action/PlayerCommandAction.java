package com.skcraft.plume.module.backtrack.action;

import com.skcraft.plume.common.service.journal.Record;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PlayerCommandAction extends PlayerChatAction {

    @Override
    public IChatComponent toQueryMessage(Record record) {
        return new ChatComponentText(String.format("%s used at %d:%d:%d: /%s",
                record.getUserId().getName(),
                record.getLocation().getX(),
                record.getLocation().getY(),
                record.getLocation().getZ(),
                getMessage()));
    }

}
