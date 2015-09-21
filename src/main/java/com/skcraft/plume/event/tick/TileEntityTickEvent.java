package com.skcraft.plume.event.tick;

import com.google.common.collect.Lists;
import com.skcraft.plume.common.util.Stopwatch;
import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import lombok.Getter;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.List;

@Cancelable
public class TileEntityTickEvent extends Event {

    @Getter private final World world;
    @Getter private final TileEntity tileEntity;
    @Getter private final List<Stopwatch> stopwatches = Lists.newArrayList();

    public TileEntityTickEvent(World world, TileEntity tileEntity) {
        this.world = world;
        this.tileEntity = tileEntity;
    }

}
