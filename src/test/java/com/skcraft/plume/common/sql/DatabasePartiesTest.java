package com.skcraft.plume.common.sql;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.party.Member;
import com.skcraft.plume.common.party.Party;
import com.skcraft.plume.common.party.PartyExistsException;
import com.skcraft.plume.common.party.Rank;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;


public class DatabasePartiesTest {

    private static final UserId LEISER_USER = new UserId(UUID.fromString("12345678-4321-1945-1111-223344556677"), "LeiserGeist");

    private MockDatabase db;

    @Before
    public void setUp() throws Exception {
        db = MockDatabase.getInstance();
    }

    private DatabaseParties createParties() {
        db.loadData();
        DatabaseManager manager = db.createDatabaseManager();
        DatabaseParties parties = new DatabaseParties(manager);
        parties.load();
        return parties;
    }

    @Test
    public void testAddParty_New() throws Exception {
        DatabaseParties partyManager = createParties();

        Party party = new Party();
        party.setName("noobs");
        party.setMembers(Sets.newHashSet(new Member(LEISER_USER, Rank.MEMBER), new Member(MockDatabase.VINCENT_USER, Rank.OWNER), new Member(MockDatabase.SK_USER, Rank.MEMBER)));
        party.setCreateTime(MockDatabase.parseDate("2012-01-02 11:12:13"));

        Party addedParty = partyManager.findPartyByName("noobs");
        assertThat(party.getName(), equalTo("noobs"));
        assertThat(party.getCreateTime(), equalTo(MockDatabase.parseDate("2012-01-02 11:12:13")));

        assertThat(party.getMembers().size(), is(3));
        assertThat(party.getMembers(), containsInAnyOrder(new Member(MockDatabase.VINCENT_USER, Rank.OWNER), new Member(LEISER_USER, Rank.MEMBER), new Member(MockDatabase.SK_USER, Rank.MEMBER)));
    }

    @Test(expected = PartyExistsException.class)
    public void testAddParty_Existing() throws Exception {
        DatabaseParties partyManager = createParties();

        Party party = new Party();
        party.setName("friends");
        party.setMembers(Sets.newHashSet(new Member(MockDatabase.VINCENT_USER, Rank.OWNER), new Member(MockDatabase.SK_USER, Rank.MEMBER)));
        party.setCreateTime(MockDatabase.parseDate("2015-03-05 10:20:30"));

        partyManager.addParty(party);
    }

    @Test
    public void testFindPartiesByName() throws Exception {
        DatabaseParties partyManager = createParties();
        Map<String, Party> parties = partyManager.findPartiesByName(Lists.newArrayList("guests", "friends"));

        assertThat(parties.size(), is(2));
        assertThat(parties.get("guests").getCreateTime(), equalTo(MockDatabase.parseDate("2015-03-05 10:20:30")));
        assertThat(parties.get("friends").getCreateTime(), equalTo(MockDatabase.parseDate("2015-02-04 10:20:30")));
    }

    @Test
    public void testFindPartyByName() throws Exception {
        DatabaseParties partyManager = createParties();
        Party party = partyManager.findPartyByName("guests");

        assertThat(party.getName(), equalTo("guests"));
        assertThat(party.getCreateTime(), equalTo(MockDatabase.parseDate("2015-03-05 10:20:30")));

        assertThat(party.getMembers().size(), is(2));
        assertThat(party.getMembers(), containsInAnyOrder(new Member(MockDatabase.VINCENT_USER, Rank.OWNER), new Member(MockDatabase.SK_USER, Rank.MEMBER)));
    }

    @Test
    public void testRefreshParty() throws Exception {
        DatabaseParties partyManager = createParties();
        Party party = partyManager.findPartyByName("guests");

        partyManager.refreshParty(party);

        assertThat(partyManager.findPartyByName("guests").getCreateTime(), equalTo(MockDatabase.parseDate("2015-03-05 10:20:30")));
    }

    @Test
    public void testAddMembers() throws Exception {
        DatabaseParties partyManager = createParties();
        Party party = partyManager.findPartyByName("friends");

        assertThat(party.getMembers().size(), is(1));

        partyManager.addMembers("friends", Sets.newHashSet(new Member(MockDatabase.VINCENT_USER, Rank.MEMBER)));
        partyManager.refreshParty(party);

        assertThat(party.getMembers().size(), is(2));
        assertThat(party.getMembers(), containsInAnyOrder(
                new Member(MockDatabase.VINCENT_USER, Rank.OWNER),
                new Member(MockDatabase.SK_USER, Rank.MEMBER)
        ));
    }

    @Test
    public void testRemoveMembers() throws Exception {
        DatabaseParties partyManager = createParties();
        Party party = partyManager.findPartyByName("guests");

        assertThat(party.getMembers().size(), is(2));

        partyManager.removeMembers("guests", Sets.newHashSet(MockDatabase.SK_USER));
        partyManager.refreshParty(party);

        assertThat(party.getMembers().size(), is(1));

    }
}
