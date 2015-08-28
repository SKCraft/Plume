package com.skcraft.plume.util;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Server {

    private Server() {
    }

    @SuppressWarnings("unchecked")
    public static List<EntityPlayerMP> getOnlinePlayers() {
        return MinecraftServer.getServer().getConfigurationManager().playerEntityList;
    }

    @Nullable
    public static EntityPlayerMP findPlayer(String name) {
        checkNotNull(name, "name");
        return MinecraftServer.getServer().getConfigurationManager().func_152612_a(name);
    }

    @Nullable
    public static EntityPlayerMP findPlayer(UUID uuid) {
        checkNotNull(uuid, "uuid");
        for (EntityPlayerMP player : getOnlinePlayers())
            if (player.getGameProfile().getId().equals(uuid))
                return player;

        return null;
    }

    public static void kick(EntityPlayerMP player, String message) {
        checkNotNull(player, "player");
        checkNotNull(message, "message");
        player.playerNetServerHandler.kickPlayerFromServer(message);
    }

    public static void shutdown(String message) {
        checkNotNull(message, "message");

        for (String name : MinecraftServer.getServer().getAllUsernames()) {
            if (name != null) {
                kick(Server.findPlayer(name), message);
            }
        }

        MinecraftServer.getServer().initiateShutdown();
    }
}
