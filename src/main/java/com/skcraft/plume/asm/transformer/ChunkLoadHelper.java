package com.skcraft.plume.asm.transformer;

import com.skcraft.plume.event.world.ChunkLoadRequestEvent;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;

public final class ChunkLoadHelper {

    private ChunkLoadHelper() {
    }

    public static boolean mayLoadChunk(WorldServer world, int x, int z) {
        ChunkLoadRequestEvent event = new ChunkLoadRequestEvent(world, x, z);
        MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

}
