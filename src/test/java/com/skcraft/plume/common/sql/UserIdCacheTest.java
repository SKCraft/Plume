package com.skcraft.plume.common.sql;

import com.skcraft.plume.common.UserId;
import org.jooq.DSLContext;
import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class UserIdCacheTest {

    private final TestDatabase db = TestDatabase.getPrimary();

    @Test
    public void testGetOrCreateUserId() throws Exception {
        db.setupDatabase();
        DatabaseManager manager = new DatabaseManager(db.getDataSource(), db.getDatabase());
        UserIdCache cache = new UserIdCache();
        DSLContext create = manager.create();
        assertThat(cache.getOrCreateUserId(create, new UserId(UUID.fromString("4da29664-a697-48ff-b702-19f8d304e461"), "test")), is(3));
        assertThat(cache.getOrCreateUserId(create, new UserId(UUID.fromString("0ea8eca3-dbf6-47cc-9d1a-c64551ca975c"), "sk89q")), is(1));
        assertThat(cache.getOrCreateUserId(create, new UserId(UUID.fromString("4da29664-a697-48ff-b702-19f8d304e461"), "test")), is(3));
    }
}
