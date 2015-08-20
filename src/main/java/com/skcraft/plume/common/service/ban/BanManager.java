package com.skcraft.plume.common.service.ban;

import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.UserId;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Manages a list of bans.
 */
public interface BanManager {

    /**
     * Load data necessary to use the object. Calling this again refreshes
     * the data.
     *
     * @throws DataAccessException Thrown if data can't be accessed
     */
    default void load() {
    }

    /**
     * Get a list of active bans for the given user.
     *
     * @param userId The user's UUID
     * @return A list of active bans
     * @throws DataAccessException Thrown on a data access error
     */
    List<Ban> findActiveBans(UserId userId);

    /**
     * Add a ban to the database.
     *
     * @param ban The ban
     * @return The case ID, if one exists, otherwise -1 is returned
     * @throws DataAccessException Thrown on a data access error
     */
    int addBan(Ban ban);

    /**
     * Pardon the given user by updating all the active bans for a user
     * with the given information.
     *
     * @param user The user ID
     * @param pardonUser The pardon user ID
     * @param pardonReason The reason for the pardon (optional)
     * @throws DataAccessException Thrown on a data access error
     */
    void pardon(UserId user, @Nullable UserId pardonUser, @Nullable String pardonReason);

}
