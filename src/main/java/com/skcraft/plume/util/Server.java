package com.skcraft.plume.util;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.skcraft.plume.common.UserId;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Server {

    private Server() {
    }

    public static WorldServer getDimensionOrLoad(int dimension) {
        return MinecraftServer.getServer().worldServerForDimension(dimension);
    }

    public static boolean isOp(UserId userId) {
        return isOp(new GameProfile(userId.getUuid(), userId.getName()));
    }

    public static boolean isOp(GameProfile gameProfile) {
        return MinecraftServer.getServer().getConfigurationManager().func_152596_g(gameProfile);
    }

    @SuppressWarnings("unchecked")
    public static List<EntityPlayerMP> getOnlinePlayers() {
        return MinecraftServer.getServer().getConfigurationManager().playerEntityList;
    }

    public static List<EntityPlayerMP> getNearbyPlayers(World world, double x, double y, double z, int radius) {
        List<EntityPlayerMP> players = Lists.newArrayList();
        int radiusSq = radius * radius;
        for (EntityPlayerMP player : getOnlinePlayers()) {
            double xd = x - player.posX;
            double yd = y - player.posY;
            double zd = z - player.posZ;
            if (player.worldObj == world && xd * xd + yd * yd + zd * zd < radiusSq) {
                players.add(player);
            }
        }
        return players;
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
