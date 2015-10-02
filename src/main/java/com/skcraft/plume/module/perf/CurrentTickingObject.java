package com.skcraft.plume.module.perf;

import com.skcraft.plume.common.util.Stopwatch;
import com.skcraft.plume.common.util.event.Order;
import com.skcraft.plume.common.util.event.Subscribe;
import com.skcraft.plume.common.util.module.AutoRegister;
import com.skcraft.plume.event.tick.EntityTickEvent;
import com.skcraft.plume.event.tick.TileEntityTickEvent;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

@AutoRegister
public class CurrentTickingObject {

    @Nullable
    @Getter
    private Object currentObject;

    @Nullable
    public TileEntity getCurrentTileEntity() {
        Object current = currentObject;
        return current instanceof TileEntity ? (TileEntity) current : null;
    }

    @Nullable
    public Entity getCurrentEntity() {
        Object current = currentObject;
        return current instanceof Entity ? (Entity) current : null;
    }

    @Subscribe(order = Order.VERY_EARLY)
    public void onTileEntityTick(TileEntityTickEvent event) {
        event.getStopwatches().add(new Stopwatch() {
            @Override
            public void start() {
                currentObject = event.getTileEntity();
            }

            @Override
            public void stop() {
                currentObject = null;
            }
        });
    }

    @Subscribe(order = Order.VERY_EARLY)
    public void onEntityTick(EntityTickEvent event) {
        event.getStopwatches().add(new Stopwatch() {
            @Override
            public void start() {
                currentObject = event.getEntity();
            }

            @Override
            public void stop() {
                currentObject = null;
            }
        });
    }

}
