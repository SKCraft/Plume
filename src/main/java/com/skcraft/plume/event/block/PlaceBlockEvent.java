package com.skcraft.plume.event.block;

import com.skcraft.plume.event.Cause;
import net.minecraft.world.World;

import java.util.List;

public class PlaceBlockEvent extends ChangeBlockEvent {

    public PlaceBlockEvent(Cause cause, World world, List<BlockChange> blocks) {
        super(cause, world, blocks);
    }

}
