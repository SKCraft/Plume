package com.skcraft.plume.util.profile;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import com.sk89q.squirrelid.Profile;
import com.sk89q.squirrelid.resolver.HttpRepositoryService;
import com.skcraft.plume.common.UserId;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

                @SuppressWarnings("unchecked")
                @Override
                public Map<String, Optional<UserId>> loadAll(Iterable<? extends String> keys) throws Exception {
                    ImmutableList<Profile> profiles = resolver.findAllByName((Iterable<String>) keys);
                    Map<String, Optional<UserId>> entries = Maps.newHashMap();
                    for (Profile profile : profiles) {
                        // Lowercase here is ugly
                        entries.put(profile.getName().toLowerCase(), Optional.fromNullable(new UserId(profile.getUniqueId(), profile.getName())));
                    }
                    for (String key : keys) {
                        if (!entries.containsKey(key)) {
                            entries.put(key, Optional.absent());
                        }
                    }
                    return entries;
                }
            });

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

    public Map<String, Optional<UserId>> findUserIds(List<String> names) throws ProfileLookupException, ProfileNotFoundException {
        try {
            return cache.getAll(names.stream().map(String::toLowerCase).collect(Collectors.toList()));
        } catch (ExecutionException e) {
            throw new ProfileLookupException(e.getCause(), Joiner.on(", ").join(names));
        }
    }

    public Callable<UserId> createFetch(String name) {
        return () -> findUserId(name);
    }

}
