package com.skcraft.plume.asm.transformer;

import com.skcraft.plume.common.util.Stopwatch;
import com.skcraft.plume.common.util.event.PlumeEventBus;
import com.skcraft.plume.event.tick.EntityTickEvent;
import com.skcraft.plume.event.tick.EntityTickExceptionEvent;
import com.skcraft.plume.event.tick.TileEntityTickEvent;
import com.skcraft.plume.event.tick.TileEntityTickExceptionEvent;
import lombok.extern.java.Log;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.world.World;

import java.util.List;
import java.util.logging.Level;

@SuppressWarnings("ForLoopReplaceableByForEach")
@Log
public final class TickCallback {

    private TickCallback() {
    }

    public static void tickEntity(Entity entity, World world) {
        EntityTickEvent tickEvent = new EntityTickEvent(world, entity);
        PlumeEventBus.INSTANCE.post(tickEvent, false);
        if (!tickEvent.isCancelled()) {
            List<Stopwatch> stopwatches = tickEvent.getStopwatches();
            try {
                for (int i = 0; i < stopwatches.size(); i++) {
                    try {
                        stopwatches.get(i).start();
                    } catch (Throwable t) {
                        log.log(Level.WARNING, "Failed to start stopwatch " + stopwatches.get(i).getClass().getName(), t);
                    }
                }
                world.updateEntity(entity);
            } catch (Throwable t) {
                EntityTickExceptionEvent exceptionEvent = new EntityTickExceptionEvent(world, entity, t);
                PlumeEventBus.INSTANCE.post(exceptionEvent);
                if (!exceptionEvent.isCancelled()) {
                    throw t;
                }
            } finally {
                for (int i = stopwatches.size() - 1; i >= 0; i--) {
                    try {
                        stopwatches.get(i).stop();
                    } catch (Throwable t) {
                        log.log(Level.WARNING, "Failed to stop stopwatch " + stopwatches.get(i).getClass().getName(), t);
                    }
                }
            }
        }
    }

    public static void tickTileEntity(TileEntity tileEntity, World world) {
        TileEntityTickEvent tickEvent = new TileEntityTickEvent(world, tileEntity);
        PlumeEventBus.INSTANCE.post(tickEvent, false);
        if (!tickEvent.isCancelled()) {
            List<Stopwatch> stopwatches = tickEvent.getStopwatches();
            try {
                ThreadDeath threadDeath = null;

                try {
                    for (int i = 0; i < stopwatches.size(); i++) {
                        try {
                            stopwatches.get(i).start();
                        } catch (ThreadDeath t) {
                            threadDeath = t;
                        } catch (Throwable t) {
                            log.log(Level.WARNING, "Failed to start stopwatch " + stopwatches.get(i).getClass().getName(), t);
                        }
                    }
                    ((ITickable) tileEntity).update();
                } finally {
                    for (int i = stopwatches.size() - 1; i >= 0; i--) {
                        try {
                            stopwatches.get(i).stop();
                        } catch (ThreadDeath t) {
                            threadDeath = t;
                        } catch (Throwable t) {
                            log.log(Level.WARNING, "Failed to stop stopwatch " + stopwatches.get(i).getClass().getName(), t);
                        }
                    }
                }

                if (threadDeath != null) {
                    throw threadDeath;
                }
            } catch (Throwable t) {
                TileEntityTickExceptionEvent exceptionEvent = new TileEntityTickExceptionEvent(world, tileEntity, t);
                PlumeEventBus.INSTANCE.post(exceptionEvent);
                if (!exceptionEvent.isCancelled()) {
                    throw t;
                }
            }
        }
    }

}
