package com.skcraft.plume.common.service.claim;

import com.skcraft.plume.common.service.party.Party;
import com.skcraft.plume.common.util.WorldVector3i;

import javax.annotation.Nullable;

/**
 * Claim entries are created by {@link ClaimCache} and provide access to
 * the underlying claim.
 */
public interface ClaimEntry {

    /**
     * Get the chunk position of the claim.
     *
     * @return The chunk position
     */
    WorldVector3i getPosition();

    /**
     * Get the claim.
     *
     * @return The claim
     */
    Claim getClaim();

    /**
     * Get the party.
     *
     * @return The party
     */
    @Nullable
    Party getParty();

    /**
     * Get whether the entry is loaded.
     *
     * @return If the entry is loaded
     */
    boolean isLoaded();

}
