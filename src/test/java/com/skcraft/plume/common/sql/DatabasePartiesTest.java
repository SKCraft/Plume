package com.skcraft.plume.common.sql;

import com.skcraft.plume.common.party.Party;
import com.skcraft.plume.common.party.PartyExistsException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;


public class DatabasePartiesTest {

    private final TestDatabase db = TestDatabase.getPrimary();

    private DatabaseParties createParties() {
        db.setupDatabase();
        DatabaseManager manager = new DatabaseManager(db.getDataSource(), db.getDatabase());
        DatabaseParties parties = new DatabaseParties(manager);
        parties.load();
        return parties;
    }

    @Test
    public void testAddParty_New() throws Exception {

    }

    @Test(expected = PartyExistsException.class)
    public void testAddParty_Existing() throws Exception {

    }

    @Test
    public void testFindPartiesByName() throws Exception {

    }

    @Test
    public void testFindPartyByName() throws Exception {
        DatabaseParties partyManager = createParties();
        Party party = partyManager.findPartyByName("friends");
        assertThat(party.getName(), equalTo("friends"));
        assertThat(party.getCreateTime(), equalTo(TestDatabase.parseDate("2015-02-04 10:20:30")));
    }

    @Test
    public void testRefreshParty() throws Exception {

    }

    @Test
    public void testAddMembers() throws Exception {

    }

    @Test
    public void testRemoveMembers() throws Exception {

    }
}