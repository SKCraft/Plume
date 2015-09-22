package com.skcraft.plume.module;

import com.google.inject.Inject;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Location3d;
import com.skcraft.plume.util.Locations;
import com.skcraft.plume.util.Materials;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.TeleportHelper;
import com.skcraft.plume.util.concurrent.TickExecutorService;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "enderpearl-homes", desc = "Teleport to your own bed or spawn using just Ender pearls")
public class EnderPearlHomes {

    private static final int HOME_DISTANCE_SQ = 5 * 5;
    private static final int SPAWN_HORIZ_DISTANCE_SQ = 3 * 3;
    private static final int SPAWN_HEAD_BUFFER = 3;

    @Inject private TickExecutorService tickExecutor;

    @SubscribeEvent
    public void onEnderTeleport(EnderTeleportEvent event) {
        if (event.entity instanceof EntityPlayerMP) {
            World world = event.entity.worldObj;
            World spawnWorld = MinecraftServer.getServer().getEntityWorld();
            EntityPlayerMP player = (EntityPlayerMP) event.entity;
            Location3d target = new Location3d(world, event.targetX, event.targetY, event.targetZ);
            Location3d current = Locations.getLocation3d(player);

            boolean inWater = Materials.isWater(world.getBlock((int) player.posX, (int) player.posY, (int) player.posZ));
            double distanceSq = current.distanceSq(target);
            double horizDistanceSq =  current.setY(0).distanceSq(target.setY(0));

            if (inWater && (distanceSq <= HOME_DISTANCE_SQ || horizDistanceSq < SPAWN_HORIZ_DISTANCE_SQ && current.getY() <= target.getY() + SPAWN_HEAD_BUFFER)) {
                ChunkCoordinates coords = spawnWorld.getSpawnPoint();
                event.setCanceled(true);
                tickExecutor.execute(() -> {
                    TeleportHelper.teleport(player, Locations.getLocation3d(spawnWorld, coords));
                    player.addChatMessage(Messages.info(tr("enderpearlHomes.toSpawn")));
                });
            } else if (current.distanceSq(target) <= HOME_DISTANCE_SQ) {
                ChunkCoordinates coords = player.getBedLocation(spawnWorld.provider.dimensionId);
                if (coords != null) {
                    tickExecutor.execute(() -> {
                        TeleportHelper.teleport(player, Locations.getLocation3d(spawnWorld, coords));
                        player.addChatMessage(Messages.info(tr("enderpearlHomes.nowAtBed")));
                    });
                } else {
                    player.addChatMessage(Messages.error(tr("enderpearlHomes.noBed")));
                }
                event.setCanceled(true);
            }
        }
    }

}
