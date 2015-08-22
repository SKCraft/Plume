package com.skcraft.plume.module.claim;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.CommandException;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Optional;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.regions.Region;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.claim.Claim;
import com.skcraft.plume.common.service.claim.ClaimCache;
import com.skcraft.plume.common.util.Vectors;
import com.skcraft.plume.common.util.WorldVector3i;
import com.skcraft.plume.common.util.concurrent.Deferred;
import com.skcraft.plume.common.util.concurrent.Deferreds;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.common.util.service.InjectService;
import com.skcraft.plume.common.util.service.Service;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.Profiles;
import com.skcraft.plume.util.Worlds;
import com.skcraft.plume.util.concurrent.BackgroundExecutor;
import com.skcraft.plume.util.concurrent.TickExecutorService;
import com.skcraft.plume.util.inventory.Inventories;
import com.skcraft.plume.util.inventory.InventoryView;
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

@Module(name = "claim-commands")
public class Claims {

    @InjectConfig("claims") private Config<ClaimConfig> config;
    @InjectService private Service<ClaimCache> claimCache;
    @Inject private TickExecutorService tickExecutorService;
    @Inject private BackgroundExecutor bgExecutor;
    private final ListeningExecutorService claimExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    private final Cache<UserId, ClaimRequest> pendingRequests = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();

    @Command(aliases = "claim", desc = "Claim a section of land")
    @Require("plume.claims.claim")
    public void claim(EntityPlayer player, @Optional String party) throws CommandException {
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

                    // Then build a claim request based on the unclaimed chunks
                    // Filter out chunks that are already owned by the player or other players
                    ClaimRequest request = new ClaimRequest(claimCache, owner, party);
                    request.addPositions(positions);
                    request.checkQuota(config.get().limits.totalClaimsMax);

                    if (!request.hasUnclaimed()) {
                        throw new ClaimAttemptException(tr("claims.noUnclaimedChunksSelected", request.getAlreadyOwned().size()));
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
    public void acceptClaim(EntityPlayer player) {
        ClaimCache claimCache = this.claimCache.provide();
        UserId owner = Profiles.fromPlayer(player);
        ClaimRequest existingRequest = pendingRequests.getIfPresent(owner);

        if (existingRequest != null) {
            pendingRequests.invalidate(owner);

            Deferred<?> deferred = Deferreds
                    .when(() -> {
                        // Rebuild a claim request because there may have been changes to the claims
                        ClaimRequest request = new ClaimRequest(claimCache, existingRequest.getOwner(), existingRequest.getParty());
                        request.addPositions(existingRequest.getUnclaimed());
                        request.checkQuota(config.get().limits.totalClaimsMax);

                        if (!request.hasUnclaimed()) { // But it could turn out that there are no unclaimed chunks left
                            throw new ClaimAttemptException(tr("claims.unclaimedNowTaken"));
                        }

                        return request;
                    }, claimExecutor)
                    .filter(request -> {
                        int unclaimedCount = request.getUnclaimed().size();
                        int gratisClaimCount = Math.max(0, config.get().cost.gratisChunkCount - request.getCurrentTotalOwnedCount());
                        int cost = Math.max(0, unclaimedCount - gratisClaimCount) * config.get().cost.itemAmount;

                        InventoryView inventory = new InventoryView(player.inventory);
                        int has = inventory.getCountOf(config.get().cost.item);

                        if (has < cost) {
                            pendingRequests.put(owner, request);
                            throw new ClaimAttemptException(tr("claims.cannotAfford"));
                        }

                        List<ItemStack> removed = inventory.remove(config.get().cost.item, cost);
                        inventory.markDirty();

                        return new Pair<>(request, removed);
                    }, tickExecutorService)
                    .filter(pair -> {
                        ClaimRequest request = pair.getValue0();
                        List<ItemStack> removed = pair.getValue1();

                        try {
                            List<Claim> claims = claimCache.getClaimMap().saveClaim(request.getUnclaimed(), request.getOwner(), request.getParty());
                            claimCache.putClaims(claims);
                            return request;
                        } catch (Exception e) {
                            throw new TransactionException(e, removed);
                        }
                    }, claimExecutor)
                    .done(request -> {
                        player.addChatMessage(Messages.info(tr("claims.purchaseSuccessful", request.getUnclaimed().size())));
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
    public void cancelClaim (EntityPlayer player){
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
    public void unclaim(EntityPlayer player) throws CommandException {
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
                    player.addChatMessage(Messages.info(tr("claims.unclaimSuccessful", request.getAlreadyOwned().size())));
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
