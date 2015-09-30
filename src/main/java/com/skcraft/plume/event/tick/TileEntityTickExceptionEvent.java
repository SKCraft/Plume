package com.skcraft.plume.event.tick;

import com.skcraft.plume.common.util.event.Cancellable;
import lombok.Getter;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TileEntityTickExceptionEvent extends TickExceptionEvent implements Cancellable {

    @Getter private final World world;
    @Getter private final TileEntity tileEntity;
    private boolean cancelled = false;

    public TileEntityTickExceptionEvent(World world, TileEntity tileEntity, Throwable throwable) {
        super(throwable);
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
