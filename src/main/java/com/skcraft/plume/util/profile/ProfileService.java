package com.skcraft.plume.util.profile;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Singleton;
import com.sk89q.squirrelid.Profile;
import com.sk89q.squirrelid.resolver.HttpRepositoryService;
import com.skcraft.plume.common.UserId;

import javax.annotation.Nullable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Singleton
public class ProfileService {

    private final com.sk89q.squirrelid.resolver.ProfileService resolver = HttpRepositoryService.forMinecraft();
    private final LoadingCache<String, Optional<UserId>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .concurrencyLevel(1)
            .build(new CacheLoader<String, Optional<UserId>>() {
                @Override
                public Optional<UserId> load(String key) throws Exception {
                    Profile profile = resolver.findByName(key);
                    if (profile != null) {
                        return Optional.fromNullable(new UserId(profile.getUniqueId(), profile.getName()));
                    } else {
                        return Optional.absent();
                    }
                }
            });

    @Nullable
    public UserId findUserId(String name) throws ProfileLookupException, ProfileNotFoundException {
        try {
            Optional<UserId> optional = cache.get(name.toLowerCase());
            if (optional.isPresent()) {
                return optional.get();
            } else {
                throw new ProfileNotFoundException(name);
            }
        } catch (ExecutionException e) {
            throw new ProfileLookupException(e.getCause(), name);
        }
    }

    public Callable<UserId> createFetch(String name) {
        return () -> findUserId(name);
    }

}
