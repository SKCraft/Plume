package com.skcraft.plume.common.party;

import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.UserId;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages groups of players called "parties."
 */
public interface PartyManager {
    
    /**
     * Load data necessary to use the object. Calling this again refreshes
     * the data.
     *
     * @throws DataAccessException Thrown if data can't be accessed
     */
    default void load() {
    }

    /**
     * Get a party with the given ID.
     *
     * @param name The party ID
     * @return The party, or null if it doesn't exist
     * @throws DataAccessException Thrown if data can't be accessed
     */
    @Nullable
    Party findPartyByName(String name);

    /**
     * Get a list of parties with the given IDs.
     *
     * <p>If there is no party for the given ID, then its value will be
     * {@code null} in the returned map.</p>
     *
     * @param names The list of party IDs
     * @return A map of parties where the key responds to the party's ID in lower case
     * @throws DataAccessException Thrown if data can't be accessed
     */
    Map<String, Party> findPartiesByName(List<String> names);

    /**
     * Refresh the party and re-load its data.
     *
     * @param party Its party
     */
    void refreshParty(Party party);

    /**
     * Create a new party.
     *
     * @param party The party
     * @throws DataAccessException Thrown if data can't be accessed
     * @throws PartyExistsException If there's already an existing party with the ID
     */
    void addParty(Party party) throws PartyExistsException;

    /**
     * Add or update the given members for the given party.
     *
     * <p>If the party does not exist, {@link DataAccessException} will be thrown.</p>
     *
     * @param party The party
     * @param members The members
     */
    void addMembers(String party, Set<Member> members);

    /**
     * Remove the given members from the given party.
     *
     * @param party The party
     * @param members The members
     */
    void removeMembers(String party, Set<UserId> members);

}
