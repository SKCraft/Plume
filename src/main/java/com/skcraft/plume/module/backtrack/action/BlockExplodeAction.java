package com.skcraft.plume.module.backtrack.action;

import com.skcraft.plume.common.service.journal.Record;
import com.skcraft.plume.module.backtrack.LoggerMessages;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BlockExplodeAction extends BlockChangeAction {

    @Override
    public IChatComponent toQueryMessage(Record record) {
        return new ChatComponentText(String.format("explosion destroyed %s at %d:%d:%d",
                LoggerMessages.getBlockName(getBefore()),
                record.getLocation().getX(),
                record.getLocation().getY(),
                record.getLocation().getZ()));
    }

}
