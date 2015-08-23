package com.skcraft.plume.module.claim;

import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.sk89q.worldedit.util.eventbus.EventHandler.Priority;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.claim.Claim;
import com.skcraft.plume.common.service.claim.ClaimCache;
import com.skcraft.plume.common.service.claim.ClaimEntry;
import com.skcraft.plume.common.service.party.Parties;
import com.skcraft.plume.common.service.party.Party;
import com.skcraft.plume.common.util.WorldVector3i;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.common.util.service.InjectService;
import com.skcraft.plume.common.util.service.Service;
import com.skcraft.plume.event.Cause;
import com.skcraft.plume.event.block.BreakBlockEvent;
import com.skcraft.plume.event.block.PlaceBlockEvent;
import com.skcraft.plume.event.block.UseBlockEvent;
import com.skcraft.plume.event.entity.DamageEntityEvent;
import com.skcraft.plume.event.entity.DestroyEntityEvent;
import com.skcraft.plume.event.entity.UseEntityEvent;
import com.skcraft.plume.util.Location3i;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.Worlds;
import com.skcraft.plume.util.profile.Profiles;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "claims")
public class Claims {

    @InjectConfig("claims") private Config<ClaimConfig> config;
    @InjectService private Service<ClaimCache> claimCache;

    // Submodules
    @Inject private ClaimCommands commands;

    @Subscribe
    public void onServerStarted(FMLServerStartedEvent event) {
        for (WorldServer world : MinecraftServer.getServer().worldServers) {
            for (Object object : world.theChunkProviderServer.loadedChunks) {
                Chunk chunk = (Chunk) object;
                claimCache.provide().queueChunk(new WorldVector3i(Worlds.getWorldName(world), chunk.xPosition, 0, chunk.zPosition));
            }
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        for (Object object : ((WorldServer) event.world).theChunkProviderServer.loadedChunks) {
            Chunk chunk = (Chunk) object;
            claimCache.provide().queueChunk(new WorldVector3i(Worlds.getWorldName(event.world), chunk.xPosition, 0, chunk.zPosition));
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        claimCache.provide().invalidateChunksInWorld(Worlds.getWorldName(event.world));
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        Chunk chunk = event.getChunk();
        claimCache.provide().queueChunk(new WorldVector3i(Worlds.getWorldName(event.world), chunk.xPosition, 0, chunk.zPosition));
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        Chunk chunk = event.getChunk();
        claimCache.provide().invalidateChunk(new WorldVector3i(Worlds.getWorldName(event.world), chunk.xPosition, 0, chunk.zPosition));
    }

    public boolean mayAccess(Cause cause, ClaimEntry entry) {
        Object rootCause = cause.getRootCause();

        if (!cause.isKnown()) {
            return false;
        } else if (rootCause instanceof FakePlayer && config.get().protection.ignoreFakePlayers) {
            return true;
        } else if (rootCause instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) rootCause;
            UserId userId = Profiles.fromPlayer(player);

            Claim claim = entry.getClaim();
            if (claim.getOwner().equals(userId)) {
                return true;
            }

            Party party = entry.getParty();
            if (party != null && Parties.isMember(party, userId)) {
                return true;
            }

            return false;
        } else {
            return true;
        }
    }

    @Subscribe(priority = Priority.VERY_LATE)
    public void onPlaceBlock(PlaceBlockEvent event) {
        if (!event.isCancelled()) {
            event.filterLocations(new LocationFilter(event.getCause()), true);
        }
    }

    @Subscribe(priority = Priority.VERY_LATE)
    public void onBreakBlock(BreakBlockEvent event) {
        if (!event.isCancelled()) {
            event.filterLocations(new LocationFilter(event.getCause()), true);
        }
    }

    @Subscribe(priority = Priority.VERY_LATE)
    public void onUseBlock(UseBlockEvent event) {
        if (!event.isCancelled()) {
            event.filterLocations(new LocationFilter(event.getCause()), true);
        }
    }

    @Subscribe(priority = Priority.VERY_LATE)
    public void onDestroyEntity(DestroyEntityEvent event) {
        if (!event.isCancelled()) {
            LocationFilter filter = new LocationFilter(event.getCause());
            event.filterEntities(input -> filter.apply(new Location3i(input.worldObj, (int) input.posX, (int) input.posY, (int) input.posZ)), true);
        }
    }

    @Subscribe(priority = Priority.VERY_LATE)
    public void onUseEntity(UseEntityEvent event) {
        if (!event.isCancelled()) {
            LocationFilter filter = new LocationFilter(event.getCause());
            event.filterEntities(input -> filter.apply(new Location3i(input.worldObj, (int) input.posX, (int) input.posY, (int) input.posZ)), true);
        }
    }

    @Subscribe(priority = Priority.VERY_LATE)
    public void onDamageEntity(DamageEntityEvent event) {
        if (!event.isCancelled()) {
            LocationFilter filter = new LocationFilter(event.getCause());
            event.filterEntities(input -> filter.apply(new Location3i(input.worldObj, (int) input.posX, (int) input.posY, (int) input.posZ)), true);
        }
    }

    private class LocationFilter implements Predicate<Location3i> {
        private final Cause cause;

        private LocationFilter(Cause cause) {
            this.cause = cause;
        }

        @Override
        public boolean apply(Location3i input) {
            EntityPlayer player = cause.getFirstPlayer();
            ClaimCache claimCache = Claims.this.claimCache.provide();
            ClaimEntry entry = claimCache.getClaimIfPresent(input.toWorldVector().div(16, 16, 16));

            if (player != null) {
                if (entry != null) {
                    Claim claim = entry.getClaim();

                    if (claim == null) {
                        return true;
                    } else if (mayAccess(cause, entry)) {
                        return true;
                    } else {
                        player.addChatMessage(Messages.error(tr("claims.protection.noAccess", claim.getOwner().getName())));
                        return false;
                    }
                } else {
                    player.addChatMessage(Messages.error(tr("claims.protection.notLoaded")));
                    return false;
                }
            } else {
                return true;
            }
        }
    }


}
