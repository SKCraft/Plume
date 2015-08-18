package com.skcraft.plume.common.sql;

import com.skcraft.plume.common.ban.Ban;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DatabaseBansTest {

    private MockDatabase db;

    @Before
    public void setUp() throws Exception {
        db = MockDatabase.getInstance();
    }

    private DatabaseBans createBans() {
        db.loadData();
        DatabaseManager manager = db.createDatabaseManager();
        DatabaseBans bans = new DatabaseBans(manager);
        bans.load();
        return bans;
    }

    @Test
    public void testGetActiveBans() throws Exception {
        DatabaseBans banManager = createBans();

        List<Ban> bans;

        bans = banManager.findActiveBans(MockDatabase.SK_USER);
        assertThat(bans.size(), is(0));

        bans = banManager.findActiveBans(MockDatabase.VINCENT_USER);
        assertThat(bans.size(), is(2));
    }

}
