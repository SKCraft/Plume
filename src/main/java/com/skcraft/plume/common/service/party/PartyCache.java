package com.skcraft.plume.common.service.party;

import com.google.common.cache.CacheLoader;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.util.cache.ManagedCache;
import com.skcraft.plume.common.util.cache.ManagedCacheBuilder;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class PartyCache {

    @Getter
    private final PartyManager manager;

    private final ManagedCache<String, Party> cache = ManagedCacheBuilder.newBuilder()
            .build(new CacheLoader<String, Party>() {
                @Override
                public Party load(String key) throws Exception {
                    return manager.findPartiesByName(Lists.newArrayList(key)).get(key);
                }

                @Override
                public ListenableFuture<Party> reload(String key, Party oldValue) {
                    synchronized (writeLock) {
                        manager.refreshParty(oldValue);
                    }
                    return Futures.immediateFuture(oldValue);
                }
            });

    private final Object writeLock = new Object();

    /**
     * Create a new party cache.
     *
     * @param manager The backend party manager
     */
    public PartyCache(PartyManager manager) {
        checkNotNull(manager, "manager");
        this.manager = manager;
    }

    /**
     * Create a new party.
     *
     * @param party The party
     * @throws DataAccessException Thrown if data can't be accessed
     * @throws PartyExistsException If there's already an existing party with the ID
     */
    public void addParty(Party party) throws PartyExistsException {
        checkNotNull(party, "party");

        synchronized (writeLock) {
            try {
                manager.addParty(party);
                cache.put(party.getName().toLowerCase(), party);
            } catch (Throwable e) {
                throw e;
            }
        }
    }

    /**
     * Get a party for the given ID, preferring values from the cache but
     * otherwise fetching parties from the underlying party manager.
     *
     * @param name The name of the party
     * @return A party
     * @throws DataAccessException If the party could not be loaded
     */
    @Nullable
    public Party getParty(String name) {
        try {
            return cache.get(name.toLowerCase());
        } catch (ExecutionException e) {
            throw new DataAccessException("Could not load the party for " + name);
        }
    }

    /**
     * Add or update the given members for the given party.
     *
     * <p>If the party does not exist, {@link DataAccessException} will be thrown.</p>
     *
     * @param party The party
     * @param members The members
     */
    public void addMembers(Party party, Set<Member> members) {
        checkNotNull(party, "party");
        checkNotNull(members, "members");

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (writeLock) {
            manager.addMembers(party.getName(), members);
            Set<Member> newMembers = Sets.newHashSet(party.getMembers());
            newMembers.addAll(members);
            party.setMembers(newMembers);
        }
    }

    /**
     * Remove the given members from the given party.
     *
     * @param party The party
     * @param members The members
     */
    public void removeMembers(Party party, Set<Member> members) {
        checkNotNull(party, "party");
        checkNotNull(members, "members");

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (writeLock) {
            Set<UserId> ids = members.stream().map(Member::getUserId).collect(Collectors.toSet());
            manager.removeMembers(party.getName(), ids);
            Set<Member> newMembers = Sets.newHashSet(party.getMembers());
            newMembers.removeAll(members);
            party.setMembers(newMembers);
        }
    }


}
