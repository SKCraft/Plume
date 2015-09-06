package com.skcraft.plume.module;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.module.border.WorldBorderConfig;
import com.skcraft.plume.module.border.WorldBorderConfig.Border;
import com.skcraft.plume.module.border.WorldBorderConfig.Border.BorderType;
import com.skcraft.plume.module.border.WorldBorderConfig.Border.Threshold;
import com.skcraft.plume.util.Location3d;
import com.skcraft.plume.util.Locations;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.TeleportHelper;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import java.util.UUID;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "world-border", desc = "Configurable world border that warns and then bounces players back from it")
public class WorldBorder {
    @InjectConfig("world-border") private Config<WorldBorderConfig> config;

    private final LoadingCache<UUID, BorderSession> sessions = CacheBuilder.newBuilder().build(new CacheLoader<UUID, BorderSession>() {
        @Override
        public BorderSession load(UUID key) throws Exception {
            return new BorderSession();
        }
    });

    @SubscribeEvent
    public void playerTick(TickEvent.PlayerTickEvent event) {
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
                Location3d snapBackLocation;
                if (session.lastValidLocation != null) {
                    snapBackLocation = session.lastValidLocation;
                } else {
                    snapBackLocation = borderType.getSnapBackLocation(location, spawn, border.snapBackSize);
                }
                TeleportHelper.teleport(player, findSafeLocation(snapBackLocation));
                player.addChatMessage(Messages.error(tr("worldBorder.borderHit")));
            }
        }
    }

    @SubscribeEvent
    public void playerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        sessions.invalidate(event.player.getGameProfile().getId());
    }

    private Location3d findSafeLocation(Location3d location) {
        World world = location.getWorld();
        int y = world.getHeightValue((int) location.getX(), (int) location.getZ());
        // TODO: Check for lava and other problems
        return location.setY(y);
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
