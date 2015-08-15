package com.skcraft.plume.common.claim;

import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.util.WorldVector3i;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A claim map persists claim data and provides access to it.
 *
 * <p>Direct access to claim maps should be used to modify claims, but if
 * claim data needs to be queried for real-time claim information,
 * an instance of {@link ClaimCache} should be used so that claim data
 * can be loaded asynchronously.</p>
 */
public interface ClaimMap {

    /**
     * Load data necessary to use the object. Calling this again refreshes
     * the data.
     *
     * @throws DataAccessException Thrown if data can't be accessed
     */
    default void load() {
    }

    /**
     * Get the claim for the given chunk position.
     *
     * @param position Chunk coordinates
     * @return A claim object, or null if there was no claim set
     * @throws DataAccessException If data could not be retrieved or saved
     */
    @Nullable
    Claim findClaimByPosition(WorldVector3i position);

    /**
     * Fetch claim data for the the given chunk positions, returning
     * a map with keys corresponding to the chunk position and the
     * values reflective of the claim at that location.
     *
     * <p>If a claim does not exist at a given position, then the returned map
     * will contain {@code null} for that position.</p>
     *
     * @param positions A list of chunk coordinates
     * @return A map of chunk position => claims
     * @throws DataAccessException If data could not be retrieved or saved
     */
    Map<WorldVector3i, Claim> findClaimsByPosition(Collection<WorldVector3i> positions);

    /**
     * Set claim information for the given chunk positions, overwriting any
     * existing claims at those locations.
     *
     * <p>A list of Claim objects will be returned for the given positions.
     * The returned Claim objects may not actually entirely match the
     * given parameters if changes were made to the database during the
     * operation.</p>
     *
     * @param positions A list of chunk coordinates
     * @param owner The new owner of the claims
     * @param party An optional party to associate with the claim
     * @return A list of claim instances
     * @throws DataAccessException If data could not be retrieved or saved
     */
    List<Claim> saveClaim(Collection<WorldVector3i> positions, UserId owner, @Nullable String party);

    /**
     * Update claim information for the given chunk positions, but only if
     * claims at those chunks exist and are owned by {@code existingOwner}
     * (which can be null to indicate unclaimed chunks).
     *
     * <p>A list of Claim objects will be returned for the given positions.
     * The returned Claim objects may not actually entirely match the
     * given parameters if changes were made to the database during the
     * operation.</p>
     *
     * @param positions A list of chunk coordinates
     * @param owner The new owner of the claims
     * @param party An optional party to associate with the claim
     * @param existingOwner The existing owner to match, or null to match unclaimed chunks
     * @return A list of claim instances
     * @throws DataAccessException If data could not be retrieved or saved
     */
    List<Claim> updateClaim(Collection<WorldVector3i> positions, UserId owner, @Nullable String party, @Nullable UserId existingOwner);

}
