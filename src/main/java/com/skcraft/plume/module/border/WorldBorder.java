package com.skcraft.plume.module.border;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.module.border.WorldBorderConfig.Border;
import com.skcraft.plume.module.border.WorldBorderConfig.Border.BorderType;
import com.skcraft.plume.module.border.WorldBorderConfig.Border.Threshold;
import com.skcraft.plume.util.Location3d;
import com.skcraft.plume.util.Location3i;
import com.skcraft.plume.util.Locations;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.SafeBlockFinder;
import com.skcraft.plume.util.SafeBlockFinder.NoSafeLocationException;
import com.skcraft.plume.util.TeleportHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.UUID;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "world-border", desc = "Configurable world border that warns and then bounces players back from it", enabled = false)
public class WorldBorder {
    @InjectConfig("world_border") private Config<WorldBorderConfig> config;

    private final LoadingCache<UUID, BorderSession> sessions = CacheBuilder.newBuilder().build(new CacheLoader<UUID, BorderSession>() {
        @Override
        public BorderSession load(UUID key) throws Exception {
            return new BorderSession();
        }
    });

    @SubscribeEvent
    public void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.player.worldObj.isRemote) return;

        EntityPlayerMP player = (EntityPlayerMP) event.player;
        Border border = config.get().border;
        BorderSession session = sessions.getUnchecked(player.getGameProfile().getId());
        BorderType borderType = border.borderType;
        Location3d spawn = Locations.getLocation3d(player.worldObj, player.worldObj.getSpawnPoint());

        if (session.hasMoved((int) player.posX, (int) player.posZ)) {
            Location3d location = Location3d.fromEntity(player);
            Threshold threshold = borderType.getThreshold(location, spawn, border.borderSize, border.bufferSize);

            if (threshold == Threshold.CLEAR) {
                session.nearBorder = false;
                session.lastValidLocation = location;
            } else if (threshold == Threshold.BUFFER) {
                if (!session.nearBorder) {
                    player.addChatMessage(Messages.info(tr("worldBorder.nearBorder")));
                    session.nearBorder = true;
                }
            } else if (threshold == Threshold.ESCAPED) {
                Location3d snapBackLocation = borderType.getSnapBackLocation(location, spawn, border.snapBackSize);
                TeleportHelper.teleport(player, getSnapBackLocation(snapBackLocation.toFloorLocation3i()).toCenteredLocation3d());
                player.addChatMessage(Messages.error(tr("worldBorder.borderHit")));
            }
        }
    }

    @SubscribeEvent
    public void playerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        sessions.invalidate(event.player.getGameProfile().getId());
    }

    private Location3i getSnapBackLocation(Location3i location) {
        try {
            SafeBlockFinder finder = new SafeBlockFinder();
            return finder.getWeightedLocations(location).get(0).getEntry();
        } catch (NoSafeLocationException e) {
            return Locations.getLocation3i(location.getWorld(), location.getWorld().getSpawnPoint());
        }
    }

    private static class BorderSession {
        private boolean moved = false;
        private int lastPosX;
        private int lastPosZ;
        private boolean nearBorder;
        private Location3d lastValidLocation;

        public boolean hasMoved(int x, int z) {
            if (!moved) {
                lastPosX = x;
                lastPosZ = z;
                moved = true;
                return true;
            } else if (x != lastPosX || z != lastPosZ) {
                lastPosX = x;
                lastPosZ = z;
                return true;
            } else {
                return false;
            }
        }
    }
}
