package com.skcraft.plume.module.claim;

import com.google.common.collect.Sets;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.claim.Claim;
import com.skcraft.plume.common.service.claim.ClaimCache;
import com.skcraft.plume.common.util.WorldVector3i;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.skcraft.plume.common.util.SharedLocale.tr;

public class ClaimRequest {

    private final ClaimCache claimCache;
    @Getter private final UserId owner;
    @Getter @Nullable private final String party;
    @Getter private final int currentTotalOwnedCount;
    @Getter private final Set<WorldVector3i> unclaimed = Sets.newHashSet();
    @Getter private final Set<WorldVector3i> alreadyOwned = Sets.newHashSet();
    @Getter private final Set<WorldVector3i> ownedByOthers = Sets.newHashSet();

    public ClaimRequest(ClaimCache claimCache, UserId owner, String party) {
        checkNotNull(claimCache, "claimCache");
        checkNotNull(owner, "owner");
        this.claimCache = claimCache;
        this.owner = owner;
        this.party = party;
        this.currentTotalOwnedCount = claimCache.getClaimMap().getClaimCount(owner);
    }

    public void addPositions(Collection<WorldVector3i> positions) {
        Map<WorldVector3i, Claim> existing = claimCache.getClaimMap().findClaimsByPosition(positions);

        // Sort out positions into free chunks and owned chunks
        for (Map.Entry<WorldVector3i, Claim> entry : existing.entrySet()) {
            Claim claim = existing.get(entry.getKey());
            if (claim.getOwner().equals(owner)) {
                alreadyOwned.add(entry.getKey());
            } else {
                ownedByOthers.add(entry.getKey());
            }
        }

        for (WorldVector3i position : positions) {
            if (!alreadyOwned.contains(position) && !ownedByOthers.contains(position)) {
                unclaimed.add(position);
            }
        }
    }

    public void checkQuota(int max) throws ClaimAttemptException {
        int newTotal = unclaimed.size() + currentTotalOwnedCount;
        if (newTotal > max) {
            throw new ClaimAttemptException(tr("claims.tooManyOwnedChunks", max, currentTotalOwnedCount));
        }
    }

    public boolean hasUnclaimed() {
        return !unclaimed.isEmpty();
    }

    public boolean hasClaimed() {
        return !alreadyOwned.isEmpty();
    }

    public Object getPositionCount() {
        return unclaimed.size() + alreadyOwned.size() + ownedByOthers.size();
    }
}
