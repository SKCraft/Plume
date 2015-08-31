package com.skcraft.plume.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public final class Worlds {

    private Worlds() {
    }

    public static String getWorldId(World world) {
        return String.valueOf(world.provider.dimensionId);
    }

    public static World getWorldFromId(String name) throws NoSuchWorldException {
        for (WorldServer world : MinecraftServer.getServer().worldServers) {
            if (getWorldId(world).equals(name)) {
                return world;
            }
        }
        throw new NoSuchWorldException("No such world as '" + name + "'");
    }

}
