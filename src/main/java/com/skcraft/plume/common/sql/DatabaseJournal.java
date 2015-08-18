package com.skcraft.plume.common.sql;

import com.google.common.collect.*;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.Region;
import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.journal.*;
import com.skcraft.plume.common.journal.Record;
import com.skcraft.plume.common.sql.model.log.tables.records.LogWorldRecord;
import com.skcraft.plume.common.util.Order;
import com.skcraft.plume.common.util.WorldVector3i;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.skcraft.plume.common.sql.model.data.tables.UserId.USER_ID;
import static com.skcraft.plume.common.sql.model.log.tables.Log.LOG;
import static com.skcraft.plume.common.sql.model.log.tables.LogWorld.LOG_WORLD;

@Slf4j
public class DatabaseJournal implements Journal {

    private static final int UPDATE_BATCH_SIZE = 100;
    private ConcurrentMap<String, Short> worldIds = Maps.newConcurrentMap();
    @Getter private final DatabaseManager database;
    @Getter private final ActionMap actionMap;

    /**
     * Create a new instance.
     *
     * @param database The database
     * @param actionMap The action map
     */
    public DatabaseJournal(DatabaseManager database, ActionMap actionMap) {
        checkNotNull(database, "database");
        checkNotNull(actionMap, "actionMap");
        this.database = database;
        this.actionMap = actionMap;
    }

    @Override
    public void load() {
        ConcurrentMap<String, Short> worldIds = Maps.newConcurrentMap();
        List<LogWorldRecord> results = database.create().selectFrom(LOG_WORLD).fetch();
        for (LogWorldRecord result : results) {
            worldIds.put(result.getName().toLowerCase(), result.getId());
        }
        this.worldIds = worldIds;
    }

    /**
     * Get the map of as a view of cached world ID mappings.
     *
     * <p>Changes to the map will be reflected in the returned map.</p>
     *
     * @return World IDs
     */
    public Map<String, Short> getWorldIds() {
        return Collections.unmodifiableMap(worldIds);
    }

    /**
     * Save the listed worlds to the database if they don't yet exist, and then
     * fill the world ID cache with the new entries.
     *
     * @param create The DSL context
     * @param worlds The list of world names
     */
    private void saveWorldIds(DSLContext create, Collection<String> worlds) {
        checkNotNull(create, "create");
        checkNotNull(worlds, "worlds");

        List<String> missing = worlds.stream()
                .filter(s -> !worldIds.containsKey(s.toLowerCase()))
                .collect(Collectors.toList());

        if (!missing.isEmpty()) {
            for (List<String> partition : Lists.partition(missing, UPDATE_BATCH_SIZE)) {
                InsertValuesStep1<LogWorldRecord, String> query = create.insertInto(LOG_WORLD, LOG_WORLD.NAME);
                for (String world : partition) {
                    query = query.values(world);
                }
                query.onDuplicateKeyIgnore().execute();
            }

            List<LogWorldRecord> results = create.selectFrom(LOG_WORLD)
                    .where(LOG_WORLD.NAME.in(missing))
                    .fetch();

            for (LogWorldRecord result : results) {
                worldIds.put(result.getName().toLowerCase(), result.getId());
            }
        }
    }

    /**
     * Builds a jOOQ condition object from given criteria.
     *
     * @param criteria The criteria
     * @param condition The starting condition to extend
     * @return The final condition
     */
    private Condition buildCondition(Criteria criteria, Condition condition) {
        String world = criteria.getWorldName();
        if (world != null) {
            condition = condition.and(LOG_WORLD.NAME.eq(world));
        }

        Region region = criteria.getContainedWith();
        if (region != null) {
            Vector min = region.getMinimumPoint();
            Vector max = region.getMaximumPoint();
            condition = condition.and(LOG.X.greaterOrEqual(min.getBlockX()));
            condition = condition.and(LOG.Y.greaterOrEqual((short) min.getBlockY()));
            condition = condition.and(LOG.Z.greaterOrEqual(min.getBlockZ()));
            condition = condition.and(LOG.X.lessOrEqual(max.getBlockX()));
            condition = condition.and(LOG.Y.lessOrEqual((short) max.getBlockY()));
            condition = condition.and(LOG.Z.lessOrEqual(max.getBlockZ()));
        }

        Date before = criteria.getBefore();
        if (before != null) {
            condition = condition.and(LOG.TIME.lessThan(new Timestamp(before.getTime())));
        }

        Date after = criteria.getSince();
        if (after != null) {
            condition = condition.and(LOG.TIME.greaterThan(new Timestamp(after.getTime())));
        }

        UserId userId = criteria.getUserId();
        if (userId != null) {
            condition = condition.and(USER_ID.UUID.eq(userId.getUuid().toString()));
        }

        return condition;
    }

    @Override
    public List<Record> queryRecords(Criteria criteria, Order order, int limit) {
        checkNotNull(criteria, "criteria");
        checkNotNull(order, "order");
        checkArgument(limit >= 1, "limit >= 1");

        try {
            DSLContext create = database.create();

            Condition condition = buildCondition(criteria, LOG.WORLD_ID.eq(LOG_WORLD.ID).and(LOG.USER_ID.eq(USER_ID.ID)));

            List<Record> records = Lists.newArrayList();

            Cursor<org.jooq.Record> cursor = create.select(LOG.fields())
                    .select(LOG_WORLD.fields())
                    .select(USER_ID.fields())
                    .from(LOG)
                    .crossJoin(LOG_WORLD)
                    .crossJoin(USER_ID)
                    .where(condition)
                    .orderBy(order == Order.ASC ? LOG.ID.asc() : LOG.ID.desc())
                    .limit(limit)
                    .fetchLazy();

            try {
                while (cursor.hasNext()) {
                    org.jooq.Record result = cursor.fetchOne();
                    Action action = actionMap.parse(result.getValue(LOG.ACTION), result.getValue(LOG.DATA));
                    if (action != null) {
                        Record record = new Record();
                        record.setId(result.getValue(LOG.ID));
                        record.setLocation(new WorldVector3i(result.getValue(LOG_WORLD.NAME), result.getValue(LOG.X), result.getValue(LOG.Y), result.getValue(LOG.Z)));
                        record.setTime(result.getValue(LOG.TIME));
                        record.setUserId(database.getUserIdCache().fromRecord(result, USER_ID));
                        record.setAction(action);
                        records.add(record);
                    } // TODO: Do something about actions that fail?
                }

                return records;
            } finally {
                cursor.close();
            }
        } catch (org.jooq.exception.DataAccessException e) {
            throw new DataAccessException("Failed to retrieve records", e);
        }
    }

    @Override
    public void addRecords(Collection<Record> records) {
        checkNotNull(records, "records");

        try {
            Set<UserId> userIds = Sets.newHashSet();
            Map<UserId, Integer> resolvedUsers;
            Set<String> worlds = Sets.newHashSet();
            DSLContext context = database.create();

            // Collect user IDs and world IDs for separate insertion
            for (Record record : records) {
                userIds.add(record.getUserId());
                worlds.add(record.getLocation().getWorldName().toLowerCase());
            }

            // Insert user IDs
            resolvedUsers = database.getUserIdCache().getOrCreateUserIds(context, userIds);

            // Insert world IDs
            saveWorldIds(context, worlds);

            context.transaction(configuration -> {
                DSLContext create = DSL.using(configuration);
                RenderContext ctx = create.renderContext().qualify(false);

                UnmodifiableIterator<List<Record>> it = Iterators.partition(records.iterator(), UPDATE_BATCH_SIZE);
                while (it.hasNext()) {
                    List<Record> partition = it.next();

                    StringBuilder builder = new StringBuilder();
                    List<Object> values = Lists.newArrayList();

                    builder.append("INSERT INTO ");
                    builder.append(create.render(LOG));
                    builder.append(" (");
                    builder.append(ctx.render(LOG.TIME)).append(", ");
                    builder.append(ctx.render(LOG.USER_ID)).append(", ");
                    builder.append(ctx.render(LOG.WORLD_ID)).append(", ");
                    builder.append(ctx.render(LOG.Z)).append(", ");
                    builder.append(ctx.render(LOG.Y)).append(", ");
                    builder.append(ctx.render(LOG.Z)).append(", ");
                    builder.append(ctx.render(LOG.ACTION)).append(", ");
                    builder.append(ctx.render(LOG.DATA));
                    builder.append(") VALUES ");

                    boolean first = true;
                    for (Record record : partition) {
                        Integer userId = resolvedUsers.get(record.getUserId());
                        Short worldId = worldIds.get(record.getLocation().getWorldName().toLowerCase());
                        WorldVector3i location = record.getLocation();
                        Action action = record.getAction();
                        Short actionId = actionMap.getId(action);

                        if (userId == null) throw new IllegalStateException("User ID resolution failed for " + record.getUserId());
                        if (worldId == null) throw new IllegalStateException("World resolution failed for " + record.getLocation());

                        if (actionId != null) {
                            if (first) {
                                first = false;
                            } else {
                                builder.append(", ");
                            }
                            builder.append("(?, ?, ?, ?, ?, ?, ?, ?)");

                            values.add(record.getTime());
                            values.add(userId);
                            values.add(worldId);
                            values.add(location.getX());
                            values.add((short) location.getY());
                            values.add(location.getZ());
                            values.add(actionId);
                            values.add(action.writeData());
                        } else {
                            log.warn("Don't know how to store the action " + action.getClass().getName());
                        }
                    }

                    create.execute(builder.toString(), values.toArray(new Object[values.size()]));
                }
            });
        } catch (org.jooq.exception.DataAccessException e) {
            throw new DataAccessException("Failed to save record", e);
        }
    }

}
