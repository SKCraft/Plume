package com.skcraft.plume.common.service.auth;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.event.lifecycle.ReloadEvent;
import com.skcraft.plume.common.util.ObjectCache;
import com.skcraft.plume.common.util.event.Subscribe;
import com.skcraft.plume.common.util.module.AutoRegister;
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
@AutoRegister
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

    @Subscribe
    public void onReload(ReloadEvent event) {
        refreshAll();
    }

    @Override
    public User load(UserId userId) {
        checkNotNull(userId, "userId");
        User user = getIfPresent(userId);
        if (user != null) {
            if (hive.refreshUser(user)) {
                cache.invalidate(userId);
                return null;
            }
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
        hive.load();
        hive.refreshUsers(cache.asMap().values()).forEach(cache::invalidate);
    }

}
