package com.skcraft.plume.common.service.auth;

import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.UserId;

import java.util.List;
import java.util.Map;

/**
 * A hive is a persistent store of user and authorization
 * information.
 *
 * <p>Before use, {@link #load()} should be called at least once.</p>
 */
public interface Hive {

    /**
     * Load data necessary to use the object. Calling this again refreshes
     * the data.
     *
     * @throws DataAccessException Thrown if data can't be accessed
     */
    default void load() {}

    /**
     * Get a list of groups that have already been loaded.
     *
     * @return A list of groups
     */
    List<Group> getLoadedGroups();

    /**
     * Get user entries for one or more users.
     *
     * <p>If a user does not yet exist in the database, then the user will
     * not appear in the returned map.</p>
     *
     * @param users A list of user UUIDs
     * @return A map of users that were found
     * @throws DataAccessException Thrown if data can't be accessed
     */
    Map<UserId, User> findUsersById(List<UserId> users);

    /**
     * Save a user to the hive by either creating a new entry or updating
     * an existing entry.
     *
     * @param user The user
     * @param saveGroups Whether group membership for the user should be saved
     * @throws DataAccessException Thrown if data can't be accessed
     */
    void saveUser(User user, boolean saveGroups);

}
