package com.skcraft.plume.util;

import com.google.common.collect.Multimap;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

import java.util.Map;

public final class ChunkManagerUtils {

    private ChunkManagerUtils() {
    }

    public static Map<World, Multimap<String, Ticket>> getTickets() throws NoSuchFieldException, IllegalAccessException {
        return ReflectionUtils.getStaticDeclaredField(ForgeChunkManager.class, "tickets");
    }

}
