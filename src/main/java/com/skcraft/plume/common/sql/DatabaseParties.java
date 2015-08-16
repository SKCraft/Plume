package com.skcraft.plume.common.sql;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.party.Member;
import com.skcraft.plume.common.party.Party;
import com.skcraft.plume.common.party.PartyExistsException;
import com.skcraft.plume.common.party.PartyManager;
import com.skcraft.plume.common.sql.model.data.tables.records.PartyRecord;
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.skcraft.plume.common.sql.model.data.tables.Party.PARTY;
import static com.skcraft.plume.common.sql.model.data.tables.PartyMember.PARTY_MEMBER;
import static com.skcraft.plume.common.sql.model.data.tables.UserId.USER_ID;

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
                Party party = record.into(Party.class);
                parties.put(party.getName(), party);
            }

            for (Record record : memberRecords) {
                Party party = parties.get(record.getValue(PARTY_MEMBER.PARTY_NAME));
                if (party != null) {
                    Member member = database.getModelMapper().map(record, Member.class);
                    member.setUserId(getDatabase().getUserIdCache().fromRecord(record, USER_ID));
                    party.getMembers().add(member);
                }
            }

            return parties;
        } catch (org.jooq.exception.DataAccessException e) {
            throw new DataAccessException("Failed to get requested parties", e);
        }
    }

    @Override
    public void refreshParty(Party party) {
        checkNotNull(party, "party");
        Party loaded = findPartyByName(party.getName());
        if (loaded != null) {
            party.setName(loaded.getName());
            party.setMembers(loaded.getMembers());
            party.setCreateTime(loaded.getCreateTime());
        }
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
