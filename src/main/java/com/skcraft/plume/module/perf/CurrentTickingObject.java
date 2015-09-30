package com.skcraft.plume.module.perf;

import com.skcraft.plume.common.util.Stopwatch;
import com.skcraft.plume.common.util.event.Order;
import com.skcraft.plume.common.util.event.Subscribe;
import com.skcraft.plume.common.util.module.AutoRegister;
import com.skcraft.plume.event.tick.TileEntityTickEvent;
import lombok.Getter;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

@AutoRegister
public class CurrentTickingObject {

    @Nullable
    @Getter
    private TileEntity currentTileEntity;

    @Subscribe(order = Order.VERY_LATE)
    public void onTileEntityTick(TileEntityTickEvent event) {
        event.getStopwatches().add(new Stopwatch() {
            @Override
            public void start() {
                currentTileEntity = event.getTileEntity();
            }

            @Override
            public void stop() {
                currentTileEntity = null;
            }
        });
    }

}
