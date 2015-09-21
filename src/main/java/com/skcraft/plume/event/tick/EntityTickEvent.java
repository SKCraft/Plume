package com.skcraft.plume.event.tick;

import com.google.common.collect.Lists;
import com.skcraft.plume.common.util.Stopwatch;
import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.util.List;

@Cancelable
public class EntityTickEvent extends Event {

    @Getter private final World world;
    @Getter private final Entity entity;
    @Getter private final List<Stopwatch> stopwatches = Lists.newArrayList();

    public EntityTickEvent(World world, Entity entity) {
        this.world = world;
        this.entity = entity;
    }

}
