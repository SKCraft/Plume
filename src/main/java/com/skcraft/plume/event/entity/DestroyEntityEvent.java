package com.skcraft.plume.event.entity;

import com.skcraft.plume.event.Cause;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.util.List;

public class DestroyEntityEvent extends EntityEvent {

    public DestroyEntityEvent(Cause cause, World world, List<Entity> entities) {
        super(cause, world, entities);
    }

}
