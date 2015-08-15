package com.skcraft.plume.common.sql;

import com.skcraft.plume.common.ban.Ban;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DatabaseBansTest {

    private final TestDatabase db = TestDatabase.getPrimary();

    private DatabaseBans createBans() {
        db.setupDatabase();
        DatabaseManager manager = new DatabaseManager(db.getDataSource(), db.getDatabase());
        DatabaseBans bans = new DatabaseBans(manager);
        bans.load();
        return bans;
    }

    @Test
    public void testGetActiveBans() throws Exception {
        DatabaseBans banManager = createBans();

        List<Ban> bans;

        bans = banManager.findActiveBans(TestDatabase.SK_USER);
        assertThat(bans.size(), is(0));

        bans = banManager.findActiveBans(TestDatabase.VINCENT_USER);
        assertThat(bans.size(), is(2));
    }

}
