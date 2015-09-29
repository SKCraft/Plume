package com.skcraft.plume.module.claim;

import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.sk89q.worldedit.util.eventbus.EventHandler.Priority;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.auth.Authorizer;
import com.skcraft.plume.common.service.auth.Context;
import com.skcraft.plume.common.service.claim.Claim;
import com.skcraft.plume.common.service.claim.ClaimCache;
import com.skcraft.plume.common.service.claim.ClaimEntry;
import com.skcraft.plume.common.service.claim.ClaimMap;
import com.skcraft.plume.common.service.party.Parties;
import com.skcraft.plume.common.service.party.Party;
import com.skcraft.plume.common.util.Environment;
import com.skcraft.plume.common.util.WorldVector3i;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.event.Cause;
import com.skcraft.plume.event.block.BreakBlockEvent;
import com.skcraft.plume.event.block.PlaceBlockEvent;
import com.skcraft.plume.event.block.UseBlockEvent;
import com.skcraft.plume.event.entity.DamageEntityEvent;
import com.skcraft.plume.event.entity.DestroyEntityEvent;
import com.skcraft.plume.event.entity.UseEntityEvent;
import com.skcraft.plume.module.perf.profiler.CollectAppendersEvent;
import com.skcraft.plume.util.Location3i;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.Worlds;
import com.skcraft.plume.util.profile.Profiles;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "claims", desc = "Checks claim protection and adds claim commands [requires claim and party services]", enabled = false)
public class Claims {

    @InjectConfig("claims") private Config<ClaimConfig> config;
    @Inject private ClaimCache claimCache;
    @Inject private ClaimMap claimMap;
    @Inject private Authorizer authorizer;
    @Inject private Environment environment;

    // Submodules
    @Inject private ClaimCommands commands;

    @Subscribe
    public void onServerStarted(FMLServerStartedEvent event) {
        for (WorldServer world : MinecraftServer.getServer().worldServers) {
            for (Object object : world.theChunkProviderServer.loadedChunks) {
                Chunk chunk = (Chunk) object;
                claimCache.queueChunk(new WorldVector3i(Worlds.getWorldId(world), chunk.xPosition, 0, chunk.zPosition));
            }
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.world.isRemote) return;

        for (Object object : ((WorldServer) event.world).theChunkProviderServer.loadedChunks) {
            Chunk chunk = (Chunk) object;
            claimCache.queueChunk(new WorldVector3i(Worlds.getWorldId(event.world), chunk.xPosition, 0, chunk.zPosition));
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) return;

        claimCache.invalidateChunksInWorld(Worlds.getWorldId(event.world));
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.world.isRemote) return;

        Chunk chunk = event.getChunk();
        claimCache.queueChunk(new WorldVector3i(Worlds.getWorldId(event.world), chunk.xPosition, 0, chunk.zPosition));
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (event.world.isRemote) return;

        Chunk chunk = event.getChunk();
        claimCache.invalidateChunk(new WorldVector3i(Worlds.getWorldId(event.world), chunk.xPosition, 0, chunk.zPosition));
    }

    @Subscribe
    public void onCollectAppenders(CollectAppendersEvent event) {
        event.getAppenders().add(new ClaimAppender(event.getTimings(), claimMap));
    }

    public boolean mayAccess(Cause cause, ClaimEntry entry) {
        Object rootCause = cause.getRootCause();
        EntityPlayer firstPlayer = cause.getFirstPlayer();

        if (!cause.isKnown()) {
            return false;
        } else if (firstPlayer instanceof FakePlayer && config.get().protection.ignoreFakePlayers) {
            return true;
        } else if (rootCause instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) rootCause;
            rootCause = Profiles.fromPlayer(player);
        }

        if (rootCause instanceof UserId) {
            UserId userId = (UserId) rootCause;
            Claim claim = entry.getClaim();

            Context.Builder builder = new Context.Builder();
            environment.update(builder);
            // TODO: Add player too
            if (authorizer.getSubject(userId).hasPermission("plume.claims.bypass", builder.build())) {
                return true;
            }

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
            event.filterLocations(new LocationFilter(event.getCause(), false), true);
        }
    }

    @Subscribe(priority = Priority.VERY_LATE)
    public void onBreakBlock(BreakBlockEvent event) {
        if (!event.isCancelled()) {
            event.filterLocations(new LocationFilter(event.getCause(), false), true);
        }
    }

    @Subscribe(priority = Priority.VERY_LATE)
    public void onUseBlock(UseBlockEvent event) {
        if (!event.isCancelled()) {
            event.filterLocations(new LocationFilter(event.getCause(), true), true);
        }
    }

    @Subscribe(priority = Priority.VERY_LATE)
    public void onDestroyEntity(DestroyEntityEvent event) {
        if (!event.isCancelled()) {
            LocationFilter filter = new LocationFilter(event.getCause(), false);
            event.filterEntities(input -> filter.apply(new Location3i(input.worldObj, (int) input.posX, (int) input.posY, (int) input.posZ)), true);
        }
    }

    @Subscribe(priority = Priority.VERY_LATE)
    public void onUseEntity(UseEntityEvent event) {
        if (!event.isCancelled()) {
            LocationFilter filter = new LocationFilter(event.getCause(), true);
            event.filterEntities(input -> filter.apply(new Location3i(input.worldObj, (int) input.posX, (int) input.posY, (int) input.posZ)), true);
        }
    }

    @Subscribe(priority = Priority.VERY_LATE)
    public void onDamageEntity(DamageEntityEvent event) {
        if (!event.isCancelled()) {
            LocationFilter filter = new LocationFilter(event.getCause(), false);
            event.filterEntities(input -> filter.apply(new Location3i(input.worldObj, (int) input.posX, (int) input.posY, (int) input.posZ)), true);
        }
    }

    @SubscribeEvent
    public void onCheckSpawn(CheckSpawn event) {
        if (event.world.isRemote) return;

        EntityLivingBase entity = event.entityLiving;

        if (config.get().systemChunksBlockMonsters && (entity instanceof EntityMob || entity instanceof EntitySlime)) {
            ClaimEntry entry = getClaimEntry(new Location3i(entity.worldObj, (int) entity.posX, (int) entity.posY, (int) entity.posZ));

            if (entry != null) {
                Claim claim = entry.getClaim();

                if (claim != null) {
                    UserId owner = claim.getOwner();
                    boolean systemOwned = owner.getUuid().equals(config.get().systemOwnerUuid);

                    if (systemOwned) {
                        event.setResult(Result.DENY);
                    }
                }
            }
        }
    }

    private ClaimEntry getClaimEntry(Location3i location) {
        ClaimCache claimCache = Claims.this.claimCache;
        WorldVector3i chunkPosition = location.toWorldVector();
        chunkPosition = new WorldVector3i(chunkPosition.getWorldId(), chunkPosition.getX() >> 4, 0, chunkPosition.getZ() >> 4);
        return claimCache.getClaimIfPresent(chunkPosition);
    }

    private class LocationFilter implements Predicate<Location3i> {
        private final Cause cause;
        private final boolean usage;

        private LocationFilter(Cause cause, boolean usage) {
            this.cause = cause;
            this.usage = usage;
        }

        @Override
        public boolean apply(Location3i input) {
            EntityPlayerMP player = cause.getFirstPlayerMP();
            ClaimEntry entry = getClaimEntry(input);

            if (entry != null) {
                Claim claim = entry.getClaim();

                if (claim == null) {
                    return true;
                } else if (mayAccess(cause, entry)) {
                    return true;
                } else {
                    UserId owner = claim.getOwner();
                    boolean systemOwned = owner.getUuid().equals(config.get().systemOwnerUuid);

                    // Allow usage for system owned claims
                    if (systemOwned && usage && config.get().systemChunksPermitUse) {
                        return true;
                    }

                    if (player != null && player.playerNetServerHandler != null) {
                        if (systemOwned) {
                            player.addChatMessage(Messages.error(tr("claims.protection.noAccessSystem")));
                        } else {
                            player.addChatMessage(Messages.error(tr("claims.protection.noAccess", claim.getOwner().getName())));
                        }
                    }
                    return false;
                }
            } else {
                if (player != null && player.playerNetServerHandler != null) {
                    player.addChatMessage(Messages.error(tr("claims.protection.notLoaded")));
                }
                return false;
            }
        }
    }


}
