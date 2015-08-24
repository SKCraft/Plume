package com.skcraft.plume.module.claim;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.CommandException;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Optional;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.regions.Region;
import com.skcraft.plume.command.At;
import com.skcraft.plume.command.Group;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.claim.Claim;
import com.skcraft.plume.common.service.claim.ClaimCache;
import com.skcraft.plume.common.service.party.Party;
import com.skcraft.plume.common.service.party.PartyCache;
import com.skcraft.plume.common.util.Vectors;
import com.skcraft.plume.common.util.WorldVector3i;
import com.skcraft.plume.common.util.concurrent.Deferred;
import com.skcraft.plume.common.util.concurrent.Deferreds;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.AutoRegister;
import com.skcraft.plume.common.util.service.InjectService;
import com.skcraft.plume.common.util.service.Service;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.Worlds;
import com.skcraft.plume.util.concurrent.BackgroundExecutor;
import com.skcraft.plume.util.concurrent.TickExecutorService;
import com.skcraft.plume.util.inventory.Inventories;
import com.skcraft.plume.util.inventory.InventoryView;
import com.skcraft.plume.util.profile.ProfileLookupException;
import com.skcraft.plume.util.profile.ProfileNotFoundException;
import com.skcraft.plume.util.profile.ProfileService;
import com.skcraft.plume.util.profile.Profiles;
import com.skcraft.plume.util.worldedit.WorldEditAPI;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.ClickEvent.Action;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import org.javatuples.Pair;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@AutoRegister
public class ClaimCommands {

    @InjectConfig("claims") private Config<ClaimConfig> config;
    @InjectService private Service<ClaimCache> claimCache;
    @InjectService private Service<PartyCache> partyCache;
    @Inject private TickExecutorService tickExecutorService;
    @Inject private BackgroundExecutor bgExecutor;
    @Inject private ProfileService profileService;
    private final ListeningExecutorService claimExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    private final Cache<UserId, ClaimRequest> pendingRequests = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();

    @Command(aliases = "claim", desc = "Claim a section of land")
    @Require("plume.claims.claim")
    public void claim(@Sender EntityPlayer player, @Optional String partyName) throws CommandException {
        ClaimCache claimCache = this.claimCache.provide();
        PartyCache partyCache = this.partyCache.provide();
        Region selection;
        UserId owner = Profiles.fromPlayer(player);
        String worldName = Worlds.getWorldName(player.worldObj);

        try {
            selection = WorldEditAPI.getSelection(player);
        } catch (IncompleteRegionException e) {
            player.addChatMessage(Messages.error(tr("claims.makeSelectionFirst")));
            return;
        }

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    // First grab a list of chunks from the player's selection
                    ClaimEnumerator enumerator = new ClaimEnumerator(config.get());
                    enumerator.setLimited(true);
                    List<WorldVector3i> positions = enumerator.enumerate(selection, input -> Vectors.fromVector2D(worldName, input));

                    if (partyName != null) {
                        Party party = partyCache.get(partyName);
                        if (party == null) {
                            throw new ClaimAttemptException(tr("claims.partyDoesNotExist", partyName));
                        }
                    }

                    // Then build a claim request based on the unclaimed chunks
                    // Filter out chunks that are already owned by the player or other players
                    ClaimRequest request = new ClaimRequest(claimCache, owner, partyName);
                    request.addPositions(positions);
                    request.checkQuota(config.get().limits.totalClaimsMax);

                    if (request.getUnclaimed().size() + request.getAlreadyOwned().size() == 0) {
                        throw new ClaimAttemptException(tr("claims.alreadyClaimedByOthers", request.getAlreadyOwned().size()));
                    }

                    pendingRequests.put(owner, request); // The user will have to accept or cancel the request

                    return request;
                }, claimExecutor)
                .done(builder -> {
                    int unclaimedCount = builder.getUnclaimed().size();
                    int ownedAlready = builder.getAlreadyOwned().size();
                    int gratisClaimCount = Math.max(0, config.get().cost.gratisChunkCount - builder.getCurrentTotalOwnedCount());
                    int cost = Math.max(0, unclaimedCount - gratisClaimCount) * config.get().cost.itemAmount;

                    IChatComponent acceptText = new ChatComponentText(tr("claims.confirm.accept"))
                            .setChatStyle(new ChatStyle().setChatClickEvent(new ClickEvent(Action.RUN_COMMAND, "/claimaccept")));
                    IChatComponent declineText = new ChatComponentText(tr("claims.confirm.cancel"))
                            .setChatStyle(new ChatStyle().setChatClickEvent(new ClickEvent(Action.RUN_COMMAND, "/claimcancel")));

                    player.addChatMessage(new ChatComponentText(tr("claims.confirm.title")));
                    player.addChatMessage(new ChatComponentText(tr("claims.confirm.count", builder.getPositionCount())));
                    player.addChatMessage(new ChatComponentText(tr("claims.confirm.ownedAlready", ownedAlready)));
                    player.addChatMessage(new ChatComponentText(tr("claims.confirm.free", unclaimedCount)));
                    player.addChatMessage(new ChatComponentText(tr("claims.confirm.cost", cost, config.get().cost.item.getItemStack().getDisplayName())));
                    player.addChatMessage(acceptText.appendSibling(new ChatComponentText(" ")).appendSibling(declineText));
                }, tickExecutorService)
                .fail(e -> {
                    if (e instanceof ClaimAttemptException) {
                        player.addChatMessage(Messages.error(e.getMessage()));
                    } else {
                        player.addChatMessage(Messages.exception(e));
                    }
                }, tickExecutorService);

        bgExecutor.notifyOnDelay(deferred, player);
    }

    @Command(aliases = "claimaccept", desc = "Cancel a pending claim request")
    public void acceptClaim(@Sender EntityPlayer player) {
        ClaimCache claimCache = this.claimCache.provide();
        PartyCache partyCache = this.partyCache.provide();
        UserId owner = Profiles.fromPlayer(player);
        ClaimRequest existingRequest = pendingRequests.getIfPresent(owner);

        if (existingRequest != null) {
            pendingRequests.invalidate(owner);

            Deferred<?> deferred = Deferreds
                    .when(() -> {
                        // Rebuild a claim request because there may have been changes to the claims
                        ClaimRequest request = new ClaimRequest(claimCache, existingRequest.getOwner(), existingRequest.getParty());
                        request.addPositions(existingRequest.getUnclaimed());
                        request.addPositions(existingRequest.getAlreadyOwned());
                        request.checkQuota(config.get().limits.totalClaimsMax);

                        if (request.getUnclaimed().size() + request.getAlreadyOwned().size() == 0) {
                            // But it could turn out that there are no unclaimed chunks left
                            throw new ClaimAttemptException(tr("claims.unclaimedNowTaken"));
                        }

                        if (request.getParty() != null) {
                            Party party = partyCache.get(request.getParty());
                            if (party == null) {
                                throw new ClaimAttemptException(tr("claims.partyDoesNotExist", request.getParty()));
                            }
                        }

                        return request;
                    }, claimExecutor)
                    .filter(request -> {
                        int unclaimedCount = request.getUnclaimed().size();
                        int gratisClaimCount = Math.max(0, config.get().cost.gratisChunkCount - request.getCurrentTotalOwnedCount());
                        int cost = Math.max(0, unclaimedCount - gratisClaimCount) * config.get().cost.itemAmount;

                        InventoryView inventory = new InventoryView(player.inventory);
                        int has = inventory.getCountOf(config.get().cost.item);
                        List<ItemStack> removed = Lists.newArrayList();

                        if (has < cost) {
                            pendingRequests.put(owner, request);
                            throw new ClaimAttemptException(tr("claims.cannotAfford"));
                        }

                        if (cost > 0) {
                            removed = inventory.remove(config.get().cost.item, cost);
                            inventory.markDirty();
                        }

                        return new Pair<>(request, removed);
                    }, tickExecutorService)
                    .filter(pair -> {
                        ClaimRequest request = pair.getValue0();
                        List<ItemStack> removed = pair.getValue1();

                        try {
                            List<WorldVector3i> positions = Lists.newArrayList();
                            positions.addAll(request.getUnclaimed());
                            positions.addAll(request.getAlreadyOwned());
                            List<Claim> claims = claimCache.getClaimMap().saveClaim(positions, request.getOwner(), request.getParty());
                            claimCache.putClaims(claims);
                            return request;
                        } catch (Exception e) {
                            throw new TransactionException(e, removed);
                        }
                    }, claimExecutor)
                    .done(request -> {
                        player.addChatMessage(Messages.info(tr("claims.purchaseSuccess", request.getUnclaimed().size())));
                    }, tickExecutorService)
                    .fail(e -> {
                        if (e instanceof ClaimAttemptException) {
                            player.addChatMessage(Messages.error(e.getMessage()));
                        } else if (e instanceof TransactionException) {
                            player.addChatMessage(Messages.exception(e));
                            ((TransactionException) e).getRemoved().forEach(itemStack -> Inventories.giveItem(player, itemStack));
                        } else {
                            player.addChatMessage(Messages.exception(e));
                        }
                    }, tickExecutorService);

            bgExecutor.notifyOnDelay(deferred, player);
        } else {
            player.addChatMessage(Messages.error(tr("claims.noPending")));
        }
    }

    @Command(aliases = "claimcancel", desc = "Cancel a pending claim request")
    public void cancelClaim (@Sender EntityPlayer player){
        UserId owner = Profiles.fromPlayer(player);
        if (pendingRequests.asMap().containsKey(owner)) {
            pendingRequests.invalidate(owner);
            player.addChatMessage(Messages.info(tr("claims.cancelledRequest")));
        } else {
            player.addChatMessage(Messages.error(tr("claims.noPending")));
        }
    }

    @Command(aliases = "unclaim", desc = "Unclaim a section of land")
    @Require("plume.claims.unclaim")
    public void unclaim(@Sender EntityPlayer player) throws CommandException {
        ClaimCache claimCache = this.claimCache.provide();
        Region selection;
        UserId owner = Profiles.fromPlayer(player);
        String worldName = Worlds.getWorldName(player.worldObj);

        try {
            selection = WorldEditAPI.getSelection(player);
        } catch (IncompleteRegionException e) {
            player.addChatMessage(Messages.error(tr("claims.makeSelectionFirst")));
            return;
        }

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    // First grab a list of chunks from the player's selection
                    ClaimEnumerator enumerator = new ClaimEnumerator(config.get());
                    enumerator.setLimited(true);
                    List<WorldVector3i> positions = enumerator.enumerate(selection, input -> Vectors.fromVector2D(worldName, input));

                    // Filter chunks
                    ClaimRequest request = new ClaimRequest(claimCache, owner, null);
                    request.addPositions(positions);

                    if (!request.hasClaimed()) {
                        throw new ClaimAttemptException(tr("claims.noClaimedChunks"));
                    }

                    claimCache.getClaimMap().removeClaims(request.getAlreadyOwned());
                    claimCache.putAsUnclaimed(request.getAlreadyOwned());

                    return request;
                }, claimExecutor)
                .done(request -> {
                    player.addChatMessage(Messages.info(tr("claims.unclaimSuccess", request.getAlreadyOwned().size())));
                }, tickExecutorService)
                .fail(e -> {
                    if (e instanceof ClaimAttemptException) {
                        player.addChatMessage(Messages.error(e.getMessage()));
                    } else {
                        player.addChatMessage(Messages.exception(e));
                    }
                }, tickExecutorService);

        bgExecutor.notifyOnDelay(deferred, player);
    }

    @Command(aliases = "claim", desc = "Claim a section of land")
    @Group(@At("claimmanage"))
    @Require("plume.claimmanage.claim")
    public void adminClaim(@Sender EntityPlayer sender, String ownerName, @Optional String partyName) throws CommandException {
        ClaimCache claimCache = this.claimCache.provide();
        PartyCache partyCache = this.partyCache.provide();
        Region selection;
        String worldName = Worlds.getWorldName(sender.worldObj);

        try {
            selection = WorldEditAPI.getSelection(sender);
        } catch (IncompleteRegionException e) {
            sender.addChatMessage(Messages.error(tr("claims.makeSelectionFirst")));
            return;
        }

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    UserId owner = profileService.findUserId(ownerName);

                    ClaimEnumerator enumerator = new ClaimEnumerator(config.get());
                    enumerator.setLimited(false);
                    List<WorldVector3i> positions = enumerator.enumerate(selection, input -> Vectors.fromVector2D(worldName, input));

                    if (partyName != null) {
                        Party party = partyCache.get(partyName);
                        if (party == null) {
                            throw new ClaimAttemptException(tr("claims.partyDoesNotExist", partyName));
                        }
                    }

                    List<Claim> claims = claimCache.getClaimMap().saveClaim(positions, owner, partyName);
                    claimCache.putClaims(claims);

                    return positions;
                }, claimExecutor)
                .done(positions -> {
                    sender.addChatMessage(Messages.info(tr("claims.adminClaimSuccess", positions.size())));
                }, tickExecutorService)
                .fail(e -> {
                    if (e instanceof ClaimAttemptException) {
                        sender.addChatMessage(Messages.error(e.getMessage()));
                    } else if (e instanceof ProfileNotFoundException) {
                        sender.addChatMessage(Messages.error(tr("args.minecraftUserNotFound", ((ProfileNotFoundException) e).getName())));
                    } else if (e instanceof ProfileLookupException) {
                        sender.addChatMessage(Messages.error(tr("args.minecraftUserLookupFailed", ((ProfileLookupException) e).getName())));
                    } else {
                        sender.addChatMessage(Messages.exception(e));
                    }
                }, tickExecutorService);

        bgExecutor.notifyOnDelay(deferred, sender);
    }

    @Command(aliases = "unclaim", desc = "Unclaim a section of land")
    @Group(@At("claimmanage"))
    @Require("plume.claimmanage.unclaim")
    public void adminUnclaim(@Sender EntityPlayer player) throws CommandException {
        ClaimCache claimCache = this.claimCache.provide();
        Region selection;
        UserId owner = Profiles.fromPlayer(player);
        String worldName = Worlds.getWorldName(player.worldObj);

        try {
            selection = WorldEditAPI.getSelection(player);
        } catch (IncompleteRegionException e) {
            player.addChatMessage(Messages.error(tr("claims.makeSelectionFirst")));
            return;
        }

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    ClaimEnumerator enumerator = new ClaimEnumerator(config.get());
                    enumerator.setLimited(true);
                    List<WorldVector3i> positions = enumerator.enumerate(selection, input -> Vectors.fromVector2D(worldName, input));

                    claimCache.getClaimMap().removeClaims(positions);
                    claimCache.putAsUnclaimed(positions);

                    return positions;
                }, claimExecutor)
                .done(positions -> {
                    player.addChatMessage(Messages.info(tr("claims.adminUnclaimSuccess", positions.size())));
                }, tickExecutorService)
                .fail(e -> {
                    if (e instanceof ClaimAttemptException) {
                        player.addChatMessage(Messages.error(e.getMessage()));
                    } else {
                        player.addChatMessage(Messages.exception(e));
                    }
                }, tickExecutorService);

        bgExecutor.notifyOnDelay(deferred, player);
    }

}
