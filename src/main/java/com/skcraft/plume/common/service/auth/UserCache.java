package com.skcraft.plume.common.service.auth;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.util.ObjectCache;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Keeps a cache of users loaded in memory for faster access, which is
 * useful for accessing user data in real-time.
 *
 * <p>If a requested user does not exist, {@code null} will be returned
 * and the result will be cached. If an error prevents a user from
 * being loaded, an exception will be thrown whenever the the user
 * is queried, but the value will be cached so the database will not be
 * hit again.</p>
 */
public class UserCache implements ObjectCache<UserId, User> {

    @Getter
    private final Hive hive;

    private final LoadingCache<UserId, User> cache = CacheBuilder.newBuilder()
            .weakValues()
            .build(new CacheLoader<UserId, User>() {
                @Override
                public User load(UserId key) throws Exception {
                    User user = hive.findUsersById(Lists.newArrayList(key)).get(key);
                    if (user != null) {
                        return user;
                    } else {
                        throw new NoSuchUserException();
                    }
                }

                @Override
                public ListenableFuture<User> reload(UserId key, User oldValue) throws Exception {
                    hive.refreshUser(oldValue);
                    return Futures.immediateFuture(oldValue);
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

    @Override
    public User load(UserId userId) {
        User user = getIfPresent(userId);
        if (user != null) {
            hive.refreshUser(user);
        } else {
            return get(userId);
        }
        return user;
    }

    @Override
    public User get(UserId userId) {
        try {
            return cache.get(userId);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof NoSuchUserException) {
                return null;
            }
            throw new DataAccessException("Couldn't get user", e.getCause());
        }
    }

    @Nullable
    @Override
    public User getIfPresent(UserId userId) {
        checkNotNull(userId, "userId");
        return cache.getIfPresent(userId);
    }

    @Override
    public void refreshAll() {
        for (UserId key : cache.asMap().keySet()) {
            cache.refresh(key);
        }
    }

}
