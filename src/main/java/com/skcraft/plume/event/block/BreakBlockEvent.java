package com.skcraft.plume.event.block;

import com.skcraft.plume.event.Cause;
import net.minecraft.world.World;

import java.util.List;

public class BreakBlockEvent extends ChangeBlockEvent {

    public BreakBlockEvent(Cause cause, World world, List<BlockChange> blocks) {
        super(cause, world, blocks);
    }

}
