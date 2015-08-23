package com.skcraft.plume.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public class TeleportHelper {

    public static void teleport(EntityPlayerMP player, Location3d dest) {
        teleport(player, dest.getX(), dest.getY(), dest.getZ(), dest.getWorld().provider.dimensionId);
    }

    public static void teleport(EntityPlayerMP player, EntityPlayerMP target) {
        teleport(player, target.posX, target.posY, target.posZ, target.worldObj.provider.dimensionId);
    }

    public static void teleport(EntityPlayerMP player, double x, double y, double z, int dimension) {
        if(player.dimension != dimension) {
            MinecraftServer.getServer().getConfigurationManager().transferPlayerToDimension(player, dimension, new BasicTeleporter(DimensionManager.getWorld(dimension)));
        }
        //TODO: allow pitch and yaw to be set
        player.playerNetServerHandler.setPlayerLocation(x, y, z, player.rotationYaw, player.rotationPitch);
    }

    public static class BasicTeleporter extends Teleporter {
        public BasicTeleporter(WorldServer world) {
            super(world);
        }

        @Override
        public boolean placeInExistingPortal(Entity entity, double x, double y, double z, float yaw) {
            return false;
        }

        @Override
        public void removeStalePortalLocations(long worldTime) {}

        @Override
        public void placeInPortal(Entity entity, double x, double y, double z, float yaw) {}
    }

}
