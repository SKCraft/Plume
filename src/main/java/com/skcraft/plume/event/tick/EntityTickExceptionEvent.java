package com.skcraft.plume.event.tick;

import com.skcraft.plume.common.util.event.Cancellable;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public class EntityTickExceptionEvent extends TickExceptionEvent implements Cancellable {

    @Getter private final World world;
    @Getter private final Entity entity;
    private boolean cancelled = false;

    public EntityTickExceptionEvent(World world, Entity entity, Throwable throwable) {
        super(throwable);
        this.world = world;
        this.entity = entity;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

}
