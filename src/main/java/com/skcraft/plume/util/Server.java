package com.skcraft.plume.util;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Server {

    private Server() {
    }

    @Nullable
    public static EntityPlayerMP findPlayer(String name) {
        checkNotNull(name, "name");
        return MinecraftServer.getServer().getConfigurationManager().func_152612_a(name);
    }

    public static void kick(EntityPlayerMP player, String message) {
        checkNotNull(player, "player");
        checkNotNull(message, "message");
        player.playerNetServerHandler.kickPlayerFromServer(message);
    }

}
