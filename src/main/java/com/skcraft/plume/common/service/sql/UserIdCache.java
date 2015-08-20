package com.skcraft.plume.common.service.sql;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.sql.model.data.tables.records.UserIdRecord;
import org.jooq.DSLContext;
import org.jooq.Record;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.skcraft.plume.common.service.sql.model.data.tables.UserId.USER_ID;

/**
 * A cache to quickly get the corresponding ID of a UserId object in
 * the database.
 */
public class UserIdCache {

    private final BiMap<UUID, Integer> userIdCache = Maps.synchronizedBiMap(HashBiMap.create());

    /**
     * Create a new UserId instance from a record using the given UserId
     * table, which could be an aliased table, and then cache the instance.
     *
     * <p>The entirety of the table should be queried for this method
     * to work as expected. The method supports cases where the user ID is null.</p>
     *
     * @param record The record containing the values
     * @param table The UserId table, which may be aliased
     * @return A UserId object, otherwise null if there was no user ID
     */
    @Nullable
    public UserId fromRecord(Record record, com.skcraft.plume.common.service.sql.model.data.tables.UserId table) {
        checkNotNull(record, "record");
        checkNotNull(table, "table");
        String uuidString = record.getValue(table.UUID);
        if (uuidString != null) {
            int id = record.getValue(table.ID);
            UUID uuid = UUID.fromString(uuidString);
            userIdCache.put(uuid, id);
            return new UserId(uuid, record.getValue(table.NAME));
        } else {
            return null;
        }
    }

    /**
     * Grab matching ID instances from the cache and put them in the given
     * map.
     *
     * @param results The map to store the matches in
     * @param userIds A collection of user IDs to match
     * @return The set of cache misses
     */
    private Set<UserId> collectCachedUserIds(Map<UserId, Integer> results, Collection<UserId> userIds) {
        checkNotNull(results, "results");
        checkNotNull(userIds, "userIds");
        Set<UserId> misses = Sets.newHashSet();
        for (UserId userId : userIds) {
            Integer id = userIdCache.get(userId.getUuid());
            if (id != null) {
                results.put(userId, id);
            } else {
                misses.add(userId);
            }
        }
        return misses;
    }

    /**
     * Fetch the list of UserIds from the database and store them in the
     * provided map, without creating new rows for missing UserIds.
     *
     * @param create The DSL context
     * @param results The map to store found matches
     * @param userIds A list of user IDs to search for
     */
    private void fetchUserIds(DSLContext create, Map<UserId, Integer> results, Collection<UserId> userIds) {
        checkNotNull(create, "create");
        checkNotNull(results, "results");
        checkNotNull(userIds, "userIds");
        List<String> uuidStrings = userIds.stream().map(userId -> userId.getUuid().toString()).collect(Collectors.toList());
        List<UserIdRecord> records = create.selectFrom(USER_ID)
                .where(USER_ID.UUID.in(uuidStrings))
                .fetch();

        for (UserIdRecord record : records) {
            UserId userId = new UserId(UUID.fromString(record.getUuid()), record.getName());
            userIdCache.put(userId.getUuid(), record.getId());
            results.put(userId, record.getId());
        }
    }

    /**
     * Get a list of IDs for the given UserIds, without creating new entries
     * for unknown user IDs.
     *
     * @param create The DSL context
     * @param userIds List of user IDs to find
     * @return A map of UserIds to ID
     */
    public Map<UserId, Integer> findUserIds(DSLContext create, Collection<UserId> userIds) {
        checkNotNull(create, "create");
        checkNotNull(userIds, "userIds");

        Map<UserId, Integer> results = Maps.newHashMap();
        Set<UserId> unknown = collectCachedUserIds(results, userIds);
        if (!unknown.isEmpty()) {
            fetchUserIds(create, results, unknown);
        }

        return results;
    }

    /**
     * Get a list of IDs for the given UserIDs, creating new entries for
     * new UserIds.
     *
     * @param create The DSL context
     * @param userIds List of user IDs to find
     * @return A map of UserIds to ID
     */
    public Map<UserId, Integer> getOrCreateUserIds(DSLContext create, Collection<UserId> userIds) {
        checkNotNull(userIds, "userIds");

        Map<UserId, Integer> results = Maps.newHashMap();
        Set<UserId> misses = collectCachedUserIds(results, userIds);
        for (UserId userId : misses) { // Not efficient for large data sets
            results.put(userId, getOrCreateUserId(create, userId));
        }

        return results;
    }

    /**
     * Get the ID for a given UserID, creating a new UserId entry in the
     * database if it's not found.
     *
     * @param create The DSL context
     * @param userId The UserId to find
     * @return The ID
     */
    public int getOrCreateUserId(DSLContext create, UserId userId) {
        checkNotNull(userId, "userId");
        UUID uuid = checkNotNull(userId.getUuid(), "userId.getUuid()");

        Integer id = userIdCache.get(uuid);
        if (id != null) {
            return id;
        }

        // Workaround for jOOQ #2123: https://github.com/jOOQ/jOOQ/issues/2123

        create.insertInto(USER_ID, USER_ID.UUID, USER_ID.NAME)
                .values(uuid.toString(), userId.getName())
                .onDuplicateKeyUpdate()
                .set(USER_ID.NAME, userId.getName())
                .execute();

        UserIdRecord result = create
                .selectFrom(USER_ID)
                .where(USER_ID.UUID.eq(uuid.toString()))
                .fetchOne();

        if (result.getId() != null) {
            id = result.getId();
            userIdCache.put(uuid, id);
            return id;
        } else {
            throw new DataAccessException("Couldn't save user ID to database -- the entry disappeared");
        }
    }

}
