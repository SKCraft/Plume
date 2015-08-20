package com.skcraft.plume.common.service.auth;

import com.google.common.cache.CacheLoader;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.util.cache.ManagedCache;
import com.skcraft.plume.common.util.cache.ManagedCacheBuilder;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Keeps a cache of users loaded in memory for faster access, which is
 * useful for accessing user data in real-time.
 *
 * <p>Users fetched through this cache will be cached for at least five
 * minutes before being discarded, unless users are fetched through
 * {@link #pin(UserId)}. Users that are "loaded" must later be
 * invalidated using {@link #unpin(UserId)} to let them be
 * discarded.</p>
 *
 * <p>If a requested user does not exist, {@code null} will be returned
 * and the result will be cached. If an error prevents a user from
 * being loaded, an exception will be thrown whenever the the user
 * is queried, but the value will be cached so the database will not be
 * hit again.</p>
 */
public class UserCache {

    @Getter
    private final Hive hive;

    private final ManagedCache<UserId, User> cache = ManagedCacheBuilder.newBuilder()
            .build(new CacheLoader<UserId, User>() {
                @Override
                public User load(UserId key) throws Exception {
                    return hive.findUsersById(Lists.newArrayList(key)).get(key);
                }
            });

    /**
     * Create a new user cache.
     *
     * @param hive The underlying users database
     */
    @Inject
    public UserCache(Hive hive) {
        checkNotNull(hive, "hive");
        this.hive = hive;
    }

    /**
     * Fetch the user from the cache, loading it from the database on miss,
     * and then mark the user to be kept in the cache indefinitely until
     * {@link #unpin(UserId)} is called.
     *
     * <p>The method will block if the user has to be loaded from the
     * underlying database.</p>
     *
     * @param userId The user ID
     * @return A user, or null if the user is missing
     * @throws DataAccessException Thrown if the user could not be loaded
     */
    @Nullable
    public User pin(UserId userId) {
        checkNotNull(userId, "userId");
        try {
            return cache.pin(userId);
        } catch (ExecutionException e) {
            throw new DataAccessException("Couldn't get user", e);
        }
    }

    /**
     * Mark a user to be later removed from caches if the user is not
     * requested in the near feature.
     *
     * @param userId The user ID
     */
    public void unpin(UserId userId) {
        checkNotNull(userId, "userId");
        cache.unpin(userId);
    }

    /**
     * Fetch the user from the cache, loading it from the database on miss.
     * Unless the user has been requested using {@link #pin(UserId)}
     * elsewhere, the returned user will eventually be evicted from the cache
     * after a time delay.
     *
     * <p>The method will block if the user has to be loaded from the
     * underlying database.</p>
     *
     * @param userId The user ID
     * @return A user, or null if the user is missing
     * @throws DataAccessException Thrown if the user could not be loaded
     */
    @Nullable
    public User getUser(UserId userId) {
        checkNotNull(userId, "userId");
        try {
            return cache.get(userId);
        } catch (ExecutionException e) {
            throw new DataAccessException("Couldn't get user", e);
        }
    }

    /**
     * Fetch the user from the cache, if it exists.
     *
     * <p>If an error occurred during loading, then </p>
     *
     * @param userId The user ID
     * @return A user, or null if the user is missing or unavailable
     */
    @Nullable
    public User getUserIfPresent(UserId userId) {
        checkNotNull(userId, "userId");
        try {
            return cache.getIfPresent(userId);
        } catch (ExecutionException e) {
            return null;
        }
    }

}
