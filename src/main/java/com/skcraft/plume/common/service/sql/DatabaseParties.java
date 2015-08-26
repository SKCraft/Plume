package com.skcraft.plume.common.service.sql;

import com.google.common.collect.*;
import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.party.Member;
import com.skcraft.plume.common.service.party.Party;
import com.skcraft.plume.common.service.party.PartyExistsException;
import com.skcraft.plume.common.service.party.PartyManager;
import com.skcraft.plume.common.service.sql.model.data.tables.records.PartyRecord;
import lombok.Getter;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.impl.DSL;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.skcraft.plume.common.service.sql.model.data.tables.Party.PARTY;
import static com.skcraft.plume.common.service.sql.model.data.tables.PartyMember.PARTY_MEMBER;
import static com.skcraft.plume.common.service.sql.model.data.tables.UserId.USER_ID;

public class DatabaseParties implements PartyManager {
    @Getter
    private final DatabaseManager database;

    public DatabaseParties(DatabaseManager database) {
        checkNotNull(database, "database");
        this.database = database;
    }

    @Override
    public void addParty(final Party party) throws PartyExistsException {
        checkNotNull(party, "party");

        try {
            database.create().transaction(configuration -> {
                DSLContext create = DSL.using(configuration);

                // Insert the party
                PartyRecord record = create.newRecord(PARTY, party);
                checkNotNull(record.getName(), "party.getName()"); // MySQL silently accepts a null

                int count = create.insertInto(PARTY)
                        .set(record)
                        .onDuplicateKeyIgnore()
                        .execute();

                if (count == 0) {
                    throw new PartyExistsException("There's already a party with the name " + party.getName());
                }

                // Add members
                addMembers(create, party.getName(), party.getMembers());
            });
        } catch (org.jooq.exception.DataAccessException e) {
            if (e.getCause() instanceof PartyExistsException) {
                throw  (PartyExistsException) e.getCause();
            }
            throw new DataAccessException("Failed to save party", e);
        }
    }

    @Nullable
    @Override
    public Party findPartyByName(String name) {
        checkNotNull(name, "name");
        return findPartiesByName(Lists.newArrayList(name)).get(name);
    }

    @Override
    public Map<String, Party> findPartiesByName(List<String> names) {
        checkNotNull(names, "names");
        return fetchParties(names, s -> new Party());
    }

    private Map<String, Party> fetchParties(Collection<String> names, Function<String, Party> partySupplier) {
        checkNotNull(names, "names");

        try {
            DSLContext create = database.create();

            Map<String, Party> parties = Maps.newHashMap();

            List<PartyRecord> partyRecords = create.selectFrom(PARTY)
                    .where(PARTY.NAME.in(names))
                    .fetch();

            List<Record> memberRecords = create
                    .select(USER_ID.fields())
                    .select(PARTY_MEMBER.fields())
                    .from(PARTY_MEMBER)
                    .crossJoin(USER_ID)
                    .where(PARTY_MEMBER.PARTY_NAME.in(names).and(PARTY_MEMBER.USER_ID.eq(USER_ID.ID)))
                    .fetch();

            for (PartyRecord record : partyRecords) {
                Party party = partySupplier.apply(record.getValue(PARTY.NAME));
                database.getModelMapper().map(record, party);
                parties.put(party.getName().toLowerCase(), party);
            }

            Multimap<Party, Member> partyMembers = HashMultimap.create();

            for (Record record : memberRecords) {
                Party party = parties.get(record.getValue(PARTY_MEMBER.PARTY_NAME).toLowerCase());
                if (party != null) {
                    Member member = database.getModelMapper().map(record, Member.class);
                    member.setUserId(getDatabase().getUserIdCache().fromRecord(record, USER_ID));
                    // Don't immediately update the party with the new members because
                    // we may be refreshing the party and so we don't want the
                    // party's state to be incorrect during loading
                    partyMembers.put(party, member);
                }
            }

            // Update each party's members from the multimap
            for (Party party : parties.values()) {
                party.setMembers(Sets.newConcurrentHashSet(partyMembers.get(party)));
            }

            return parties;
        } catch (org.jooq.exception.DataAccessException e) {
            throw new DataAccessException("Failed to get requested parties", e);
        }
    }

    @Override
    public boolean refreshParty(Party party) {
        checkNotNull(party, "party");
        return refreshParties(Lists.newArrayList(party)).contains(party.getName().toLowerCase());
    }

    @Override
    public Set<String> refreshParties(Collection<Party> parties) {
        checkNotNull(parties, "parties");
        if (parties.isEmpty()) {
            return Sets.newHashSet();
        }

        Map<String, Party> partyMap = Maps.newHashMap();
        for (Party party : parties) {
            partyMap.put(party.getName().toLowerCase(), party);
        }

        Map<String, Party> refreshed = fetchParties(partyMap.keySet(), input -> {
            Party party = partyMap.get(input.toLowerCase());
            if (party != null) {
                return party;
            } else {
                throw new IllegalStateException("SQL query returned party that wasn't supposed to be refreshed");
            }
        });

        Set<String> removed = Sets.newHashSet();
        for (Party party : parties) {
            if (!refreshed.containsKey(party.getName().toLowerCase())) {
                removed.add(party.getName().toLowerCase());
            }
        }

        return removed;
    }

    @Override
    public void addMembers(final String party, final Set<Member> members) {
        checkNotNull(party, "party");
        checkNotNull(members, "members");
        try {
            database.create().transaction(configuration -> {
                DSLContext create = DSL.using(configuration);
                addMembers(create, party, members);
            });
        } catch (org.jooq.exception.DataAccessException e) {
            throw new DataAccessException("Failed to add party members", e);
        }
    }

    @Override
    public void removeMembers(String party, Set<UserId> members) {
        try {
            DSLContext create = database.create();
            Collection<Integer> userIds = database.getUserIdCache().getOrCreateUserIds(create, members).values();

            create.deleteFrom(PARTY_MEMBER)
                    .where(PARTY_MEMBER.PARTY_NAME.eq(party)
                    .and(PARTY_MEMBER.USER_ID.in(userIds)))
                    .execute();
        } catch (org.jooq.exception.DataAccessException e) {
            throw new DataAccessException("Failed to remove party members", e);
        }
    }

    private void addMembers(DSLContext create, String party, Collection<Member> members) {
        List<UserId> userIds = Lists.newArrayList();
        for (Member member : members) {
            userIds.add(checkNotNull(member.getUserId(), "member.getUserId()"));
        }
        Map<UserId, Integer> userIdsMap = database.getUserIdCache().getOrCreateUserIds(create, userIds);

        List<Query> memberQueries = Lists.newArrayList();
        for (Member member : members) {
            int id = checkNotNull(userIdsMap.get(member.getUserId()));
            memberQueries.add(create
                    .insertInto(PARTY_MEMBER, PARTY_MEMBER.PARTY_NAME, PARTY_MEMBER.USER_ID, PARTY_MEMBER.RANK)
                    .values(party, id, member.getRank().name())
                    .onDuplicateKeyUpdate()
                    .set(PARTY_MEMBER.RANK, member.getRank().name()));
        }

        create.batch(memberQueries).execute();
    }

}
