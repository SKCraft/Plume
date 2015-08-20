package com.skcraft.plume.common.service.sql;

import com.google.common.collect.Lists;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.auth.Group;
import com.skcraft.plume.common.service.auth.User;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;

import static com.skcraft.plume.common.service.sql.MockDatabase.SK_USER;
import static com.skcraft.plume.common.service.sql.MockDatabase.VINCENT_USER;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

public class DatabaseHiveTest {

    private static final UserId NEW_USER = new UserId(UUID.fromString("570fba58-2ca2-47dd-a5a1-ffb606363c84"), "new_user");

    private MockDatabase db;

    @Before
    public void setUp() throws Exception {
        db = MockDatabase.getInstance();
    }

    private DatabaseHive createHive() {
        db.loadData();
        DatabaseManager manager = db.createDatabaseManager();
        DatabaseHive hive = new DatabaseHive(manager);
        hive.load();
        return hive;
    }

    @Test
    public void testLoad() throws Exception {
        DatabaseHive hive = createHive();

        Map<Integer, Group> groups = hive.getGroups();

        assertThat(groups.size(), is(3));
        Group admins = groups.get(1);
        Group users = groups.get(2);
        Group members = groups.get(3);

        assertThat(admins.getName(), equalTo("ADMINS"));
        assertThat(users.getName(), equalTo("USERS"));
        assertThat(members.getName(), equalTo("MEMBERS"));

        assertThat(admins.getPermissions(), containsInAnyOrder("*"));
        assertThat(users.getPermissions(), containsInAnyOrder("whitelist", "access"));
        assertThat(members.getPermissions(), containsInAnyOrder("worldedit", "cookie"));

        assertThat(admins.getParents().size(), is(0));
        assertThat(users.getParents().size(), is(0));
        assertThat(members.getParents(), containsInAnyOrder(users));
    }

    @Test
    public void testFindUsersById() throws Exception {
        DatabaseHive hive = createHive();

        Map<UserId, User> users;

        users = hive.findUsersById(Lists.newArrayList(SK_USER));
        assertThat(users.size(), is(1));
        assertThat(users.get(SK_USER).getUserId().getUuid(), equalTo(SK_USER.getUuid()));
        assertThat(users.get(SK_USER).getUserId().getName(), equalTo("sk89q"));
        assertThat(users.get(SK_USER).getHostKey(), equalTo("access"));

        users = hive.findUsersById(Lists.newArrayList(VINCENT_USER));
        assertThat(users.get(VINCENT_USER).getHostKey(), equalTo("test"));

        users = hive.findUsersById(Lists.newArrayList(SK_USER, VINCENT_USER));
        assertThat(users.size(), is(2));
    }

    @Test
    public void testSaveUser() throws Exception {
        DatabaseHive hive = createHive();

        User user = new User();
        user.setUserId(NEW_USER);
        user.setReferrer(SK_USER);
        hive.saveUser(user, true);

        Map<UserId, User> users = hive.findUsersById(Lists.newArrayList(NEW_USER));
        assertThat(users.size(), is(1));
        assertThat(users.get(NEW_USER).getUserId().getUuid(), equalTo(NEW_USER.getUuid()));
        assertThat(users.get(NEW_USER).getReferrer().getUuid(), equalTo(SK_USER.getUuid()));
    }
}
