package com.skcraft.plume.event.tick;

import cpw.mods.fml.common.eventhandler.Cancelable;
import lombok.Getter;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

@Cancelable
public class TileEntityTickExceptionEvent extends TickExceptionEvent {

    @Getter
    private final World world;
    @Getter private final TileEntity tileEntity;

    public TileEntityTickExceptionEvent(World world, TileEntity tileEntity, Throwable throwable) {
        super(throwable);
        this.world = world;
        this.tileEntity = tileEntity;
    }

}
