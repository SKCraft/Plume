package com.skcraft.plume.common.service.sql;

import com.google.common.base.Function;
import com.google.common.collect.*;
import com.google.common.collect.ImmutableMap.Builder;
import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.auth.Group;
import com.skcraft.plume.common.service.auth.Hive;
import com.skcraft.plume.common.service.auth.User;
import com.skcraft.plume.common.service.sql.model.data.tables.records.GroupParentRecord;
import com.skcraft.plume.common.service.sql.model.data.tables.records.GroupRecord;
import com.skcraft.plume.common.service.sql.model.data.tables.records.UserRecord;
import lombok.Getter;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.skcraft.plume.common.service.sql.model.data.tables.Group.GROUP;
import static com.skcraft.plume.common.service.sql.model.data.tables.GroupParent.GROUP_PARENT;
import static com.skcraft.plume.common.service.sql.model.data.tables.User.USER;
import static com.skcraft.plume.common.service.sql.model.data.tables.UserGroup.USER_GROUP;
import static com.skcraft.plume.common.service.sql.model.data.tables.UserId.USER_ID;

public class DatabaseHive implements Hive {

    private static final int QUERY_BATCH_SIZE = 200;

    @Getter
    private final DatabaseManager database;
    private ImmutableMap<Integer, Group> groups = ImmutableMap.of();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public DatabaseHive(DatabaseManager database) {
        checkNotNull(database, "database");
        this.database = database;
    }

    @Override
    public void load() throws DataAccessException {
        loadGroups();
    }

    @Override
    public List<Group> getLoadedGroups() {
        return Lists.newArrayList(groups.values());
    }

    private void loadGroups() throws DataAccessException {
        Lock lock = this.lock.writeLock();
        lock.lock();
        try {
            DSLContext create = database.create();
            // Fetch groups
            Builder<Integer, Group> groupsBuilder = ImmutableMap.builder();

            for (GroupRecord record : create.selectFrom(GROUP).fetch()) {
                Group group = new Group();
                group.setId(record.getId());
                group.setName(record.getName());
                group.setPermissions(ImmutableSet.copyOf(record.getPermissions().toLowerCase().split("\n")));
                group.setAutoJoin(record.getAutoJoin() == 1);
                groupsBuilder.put(group.getId(), group);
            }

            ImmutableMap<Integer, Group> groups = groupsBuilder.build();

            // Link up parents
            for (GroupParentRecord record : create.selectFrom(GROUP_PARENT).fetch()) {
                Group group = groups.get(record.getGroupId().intValue());
                Group parent = groups.get(record.getParentId().intValue());

                if (group != null && parent != null && !group.equals(parent)) {
                    group.getParents().add(parent);
                }
            }

            this.groups = groups;
        } catch (org.jooq.exception.DataAccessException e) {
            throw new DataAccessException("Failed to get group data", e);
        } finally {
            lock.unlock();
        }
    }

    public ImmutableMap<Integer, Group> getGroups() {
        return groups;
    }

    @Nullable
    @Override
    public User findUserById(UserId user) {
        checkNotNull(user, "user");
        return findUsersById(Lists.newArrayList(user)).get(user);
    }

    @Override
    public Map<UserId, User> findUsersById(List<UserId> ids) throws DataAccessException {
        return fetchUsers(ids, input -> new User());
    }

    private Map<UserId, User> fetchUsers(Collection<UserId> ids, Function<UserId, User> userSupplier) throws DataAccessException {
        checkNotNull(ids, "ids");
        checkArgument(!ids.isEmpty(), "empty list provided");

        Lock lock = this.lock.readLock();
        lock.lock();
        try {
            DSLContext create = database.create();

            Map<Integer, User> users = Maps.newHashMap(); // Temporary map to first get the users then add their groups

            com.skcraft.plume.common.service.sql.model.data.tables.UserId r = USER_ID.as("r");

            Map<UserId, User> map = Maps.newHashMap();

            UnmodifiableIterator<List<String>> it = Iterators.partition(ids.stream().map(id -> id.getUuid().toString()).iterator(), QUERY_BATCH_SIZE);
            while (it.hasNext()) {
                List<String> partition = it.next();

                Result<Record> userRecords = create
                        .select(USER_ID.fields())
                        .select(USER.fields())
                        .select(r.fields())
                        .from(USER_ID)
                        .crossJoin(USER)
                        .leftOuterJoin(r).on(USER.REFERRER_ID.eq(r.ID))
                        .where(USER_ID.UUID.in(partition).and(USER.USER_ID.eq(USER_ID.ID)))
                        .fetch();

                List<Record> groupRecords = create
                        .select()
                        .from(USER_ID.join(USER_GROUP).on(USER_GROUP.USER_ID.eq(USER_ID.ID)))
                        .where(USER_ID.UUID.in(partition))
                        .fetch();

                for (Record record : userRecords) {
                    UserId userId = database.getUserIdCache().fromRecord(record, USER_ID);
                    User user = userSupplier.apply(userId);
                    database.getModelMapper().map(record, user);
                    user.setUserId(userId);
                    user.setReferrer(database.getUserIdCache().fromRecord(record, r));
                    users.put(record.getValue(USER_ID.ID), user);
                }

                Multimap<User, Group> userGroups = HashMultimap.create();

                for (Record record : groupRecords) {
                    User user = users.get(record.getValue(USER_ID.ID));
                    if (user != null) {
                        Group group = groups.get(record.getValue(USER_GROUP.GROUP_ID));
                        if (group != null) {
                            // Don't immediately update the user with the new groups because
                            // we may be refreshing the user and so we don't want the
                            // user's state to be incorrect during loading
                            userGroups.put(user, group);
                        }
                    }
                }

                // Update each user's groups from the multimap
                for (User user : users.values()) {
                    user.setGroups(Sets.newConcurrentHashSet(userGroups.get(user)));
                }

                for (User user : users.values()) {
                    map.put(user.getUserId(), user);
                }
            }

            return map;
        } catch (org.jooq.exception.DataAccessException e) {
            throw new DataAccessException("Failed to get user data", e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void saveUser(User user, boolean saveGroups) throws DataAccessException {
        checkNotNull(user, "user");
        try {
            DSLContext c = database.create();

            int userId = database.getUserIdCache().getOrCreateUserId(c, user.getUserId());

            UserRecord record = c.newRecord(USER, user);
            record.setUserId(userId);

            if (user.getReferrer() != null) {
                int referrerId = database.getUserIdCache().getOrCreateUserId(c, user.getReferrer());
                record.setReferrerId(referrerId);
            }

            c.transaction(configuration -> {
                DSLContext create = DSL.using(configuration);

                create.insertInto(USER)
                        .set(record)
                        .onDuplicateKeyUpdate()
                        .set(record)
                        .execute();

                if (saveGroups) {
                    create.deleteFrom(USER_GROUP)
                            .where(USER_GROUP.USER_ID.eq(userId));

                    List<Query> queries = Lists.newArrayList();

                    for (Group group : user.getGroups()) {
                        queries.add(create.insertInto(USER_GROUP)
                                .set(USER_GROUP.GROUP_ID, group.getId())
                                .set(USER_GROUP.USER_ID, userId));
                    }

                    create.batch(queries).execute();
                }
            });
        } catch (org.jooq.exception.DataAccessException e) {
            throw new DataAccessException("Failed to save the user " + user, e);
        }
    }

    @Override
    public boolean refreshUser(User user) {
        return refreshUsers(Lists.newArrayList(user)).contains(user.getUserId());
    }

    @Override
    public Set<UserId> refreshUsers(Collection<User> users) {
        checkNotNull(users, "users");
        if (users.isEmpty()) {
            return Sets.newHashSet();
        }

        Map<UserId, User> userMap = Maps.newHashMap();
        for (User user : users) {
            userMap.put(user.getUserId(), user);
        }

        Map<UserId, User> refreshed = fetchUsers(userMap.keySet(), input -> {
            User user = userMap.get(input);
            if (user != null) {
                return user;
            } else {
                throw new IllegalStateException("SQL query returned user that wasn't supposed to be refreshed");
            }
        });

        Set<UserId> removed = Sets.newHashSet();
        for (User user : users) {
            if (!refreshed.containsKey(user.getUserId())) {
                removed.add(user.getUserId());
            }
        }

        return removed;
    }

}
