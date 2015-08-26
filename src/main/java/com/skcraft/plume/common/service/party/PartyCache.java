package com.skcraft.plume.common.service.party;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.claim.NoSuchPartyException;
import com.skcraft.plume.common.util.ObjectCache;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class PartyCache implements ObjectCache<String, Party> {

    @Getter
    private final PartyManager manager;

    private final LoadingCache<String, Party> cache = CacheBuilder.newBuilder()
            .weakValues()
            .build(new CacheLoader<String, Party>() {
                @Override
                public Party load(String key) throws Exception {
                    Party party = manager.findPartiesByName(Lists.newArrayList(key)).get(key);
                    if (party != null) {
                        return party;
                    } else {
                        throw new NoSuchPartyException();
                    }
                }
            });

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
    public void add(Party party) throws PartyExistsException {
        checkNotNull(party, "party");

        manager.addParty(party);
        load(party.getName());
    }

    @Override
    public Party load(String name) {
        Party party = cache.getIfPresent(name.toLowerCase());
        if (party != null) {
            if (manager.refreshParty(party)) {
                cache.invalidate(name.toLowerCase());
                return null;
            }
        } else {
            return get(name);
        }
        return party;
    }

    @Override
    public Party get(String name) {
        try {
            return cache.get(name.toLowerCase());
        } catch (ExecutionException e) {
            if (e.getCause() instanceof NoSuchPartyException) {
                return null;
            }
            throw new DataAccessException("Could not load the party for " + name, e.getCause());
        }
    }

    @Nullable
    @Override
    public Party getIfPresent(String key) {
        return cache.getIfPresent(key.toLowerCase());
    }

    @Override
    public void refreshAll() {
        manager.refreshParties(cache.asMap().values()).forEach(s -> cache.invalidate(s.toLowerCase()));
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

        manager.addMembers(party.getName(), members);
        load(party.getName().toLowerCase());
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

        Set<UserId> ids = members.stream().map(Member::getUserId).collect(Collectors.toSet());
        manager.removeMembers(party.getName(), ids);
        load(party.getName().toLowerCase());
    }


}
