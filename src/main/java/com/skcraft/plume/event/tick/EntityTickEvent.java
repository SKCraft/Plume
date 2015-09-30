package com.skcraft.plume.event.tick;

import com.google.common.collect.Lists;
import com.skcraft.plume.common.util.Stopwatch;
import com.skcraft.plume.common.util.event.Cancellable;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.util.List;

public class EntityTickEvent implements Cancellable {

    @Getter private final World world;
    @Getter private final Entity entity;
    @Getter private final List<Stopwatch> stopwatches = Lists.newArrayList();
    private boolean cancelled = false;

    public EntityTickEvent(World world, Entity entity) {
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
