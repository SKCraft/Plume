package com.skcraft.plume.event.tick;

import com.google.common.collect.Lists;
import com.skcraft.plume.common.util.Stopwatch;
import com.skcraft.plume.common.util.event.Cancellable;
import lombok.Getter;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.List;

public class TileEntityTickEvent implements Cancellable {

    @Getter private final World world;
    @Getter private final TileEntity tileEntity;
    @Getter private final List<Stopwatch> stopwatches = Lists.newArrayList();
    private boolean cancelled = false;

    public TileEntityTickEvent(World world, TileEntity tileEntity) {
        this.world = world;
        this.tileEntity = tileEntity;
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
