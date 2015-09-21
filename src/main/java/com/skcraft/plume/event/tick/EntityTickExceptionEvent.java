package com.skcraft.plume.event.tick;

import cpw.mods.fml.common.eventhandler.Cancelable;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

@Cancelable
public class EntityTickExceptionEvent extends TickExceptionEvent {

    @Getter private final World world;
    @Getter private final Entity entity;

    public EntityTickExceptionEvent(World world, Entity entity, Throwable throwable) {
        super(throwable);
        this.world = world;
        this.entity = entity;
    }

}
