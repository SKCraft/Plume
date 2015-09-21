package com.skcraft.plume.asm.transformer;

import com.skcraft.plume.common.util.Stopwatch;
import com.skcraft.plume.event.tick.EntityTickEvent;
import com.skcraft.plume.event.tick.EntityTickExceptionEvent;
import com.skcraft.plume.event.tick.TileEntityTickEvent;
import com.skcraft.plume.event.tick.TileEntityTickExceptionEvent;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;

@SuppressWarnings("ForLoopReplaceableByForEach")
public final class TickCallback {

    private TickCallback() {
    }

    public static void tickEntity(Entity entity, World world) {
        EntityTickEvent tickEvent = new EntityTickEvent(world, entity);
        MinecraftForge.EVENT_BUS.post(tickEvent);
        if (!tickEvent.isCanceled()) {
            List<Stopwatch> stopwatches = tickEvent.getStopwatches();
            try {
                for (int i = 0; i < stopwatches.size(); i++) {
                    stopwatches.get(i).start();
                }
                world.updateEntity(entity);
            } catch (Throwable t) {
                EntityTickExceptionEvent exceptionEvent = new EntityTickExceptionEvent(world, entity, t);
                MinecraftForge.EVENT_BUS.post(tickEvent);
                if (!exceptionEvent.isCanceled()) {
                    throw t;
                }
            } finally {
                for (int i = stopwatches.size() - 1; i >= 0; i--) {
                    stopwatches.get(i).stop();
                }
            }
        }
    }

    public static void tickTileEntity(TileEntity tileEntity, World world) {
        TileEntityTickEvent tickEvent = new TileEntityTickEvent(world, tileEntity);
        MinecraftForge.EVENT_BUS.post(tickEvent);
        if (!tickEvent.isCanceled()) {
            List<Stopwatch> stopwatches = tickEvent.getStopwatches();
            try {
                for (int i = 0; i < stopwatches.size(); i++) {
                    stopwatches.get(i).start();
                }
                tileEntity.updateEntity();
            } catch (Throwable t) {
                TileEntityTickExceptionEvent exceptionEvent = new TileEntityTickExceptionEvent(world, tileEntity, t);
                MinecraftForge.EVENT_BUS.post(tickEvent);
                if (!exceptionEvent.isCanceled()) {
                    throw t;
                }
            } finally {
                for (int i = stopwatches.size() - 1; i >= 0; i--) {
                    stopwatches.get(i).stop();
                }
            }
        }
    }

}
