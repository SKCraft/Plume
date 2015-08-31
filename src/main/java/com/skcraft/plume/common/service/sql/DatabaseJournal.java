package com.skcraft.plume.common.service.sql;

import com.google.common.collect.*;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.Region;
import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.journal.*;
import com.skcraft.plume.common.service.journal.Record;
import com.skcraft.plume.common.service.journal.criteria.Criteria;
import com.skcraft.plume.common.service.sql.model.log.tables.records.LogWorldRecord;
import com.skcraft.plume.common.util.*;
import lombok.Getter;
import lombok.extern.java.Log;
import org.jooq.*;
import org.jooq.Cursor;
import org.jooq.impl.DSL;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.skcraft.plume.common.service.sql.model.data.tables.UserId.USER_ID;
import static com.skcraft.plume.common.service.sql.model.log.tables.Log.LOG;
import static com.skcraft.plume.common.service.sql.model.log.tables.LogWorld.LOG_WORLD;

@Log
public class DatabaseJournal implements Journal {

    private static final int UPDATE_BATCH_SIZE = 100;
    private ConcurrentMap<String, Short> worldIds = Maps.newConcurrentMap();
    @Getter private final DatabaseManager database;

    /**
     * Create a new instance.
     *
     * @param database The database
     */
    public DatabaseJournal(DatabaseManager database) {
        checkNotNull(database, "database");
        this.database = database;
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
        String world = criteria.getWorldId();
        if (world != null) {
            condition = condition.and(LOG_WORLD.NAME.eq(world));
        }

        Region region = criteria.getContainedWithin();
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

        List<Short> actions = criteria.getActions();
        if (actions != null) {
            condition = condition.and(LOG.ACTION.in(actions));
        }

        List<Short> excludeActions = criteria.getExcludeActions();
        if (excludeActions != null) {
            condition = condition.and(LOG.ACTION.notIn(excludeActions));
        }

        return condition;
    }

    private Record mapJooqRecord(org.jooq.Record result) {
        if (result == null) {
            return null;
        }
        Record record = new Record();
        record.setId(result.getValue(LOG.ID));
        record.setLocation(new WorldVector3i(result.getValue(LOG_WORLD.NAME), result.getValue(LOG.X), result.getValue(LOG.Y), result.getValue(LOG.Z)));
        record.setTime(result.getValue(LOG.TIME));
        record.setUserId(database.getUserIdCache().fromRecord(result, USER_ID));
        record.setAction(result.getValue(LOG.ACTION));
        record.setData(result.getValue(LOG.DATA));
        return record;
    }

    public Cursor<org.jooq.Record> getRecordsCursor(Criteria criteria, Order order, int limit) {
        checkNotNull(criteria, "criteria");
        checkNotNull(order, "order");

        try {
            DSLContext create = database.create();

            Condition condition = buildCondition(criteria, LOG.WORLD_ID.eq(LOG_WORLD.ID));

            SelectLimitStep<org.jooq.Record> query = create.select(LOG.fields())
                    .select(LOG_WORLD.fields())
                    .select(USER_ID.fields())
                    .from(LOG)
                    .crossJoin(LOG_WORLD)
                    .leftOuterJoin(USER_ID).on(LOG.USER_ID.eq(USER_ID.ID))
                    .where(condition)
                    .orderBy(order == Order.ASC ? LOG.ID.asc() : LOG.ID.desc());

            Cursor<org.jooq.Record> cursor;

            if (limit > 1) {
                cursor = query.limit(limit).fetchLazy();
            } else {
                cursor = query.fetchLazy();
            }

            return cursor;
        } catch (org.jooq.exception.DataAccessException e) {
            throw new DataAccessException("Failed to retrieve records", e);
        }
    }

    @Override
    public com.skcraft.plume.common.util.Cursor<Record> findRecords(Criteria criteria, Order order) {
        checkNotNull(criteria, "criteria");
        checkNotNull(order, "order");

        try {
            return new RecordCursor(getRecordsCursor(criteria, order, -1));
        } catch (org.jooq.exception.DataAccessException e) {
            throw new DataAccessException("Failed to retrieve records", e);
        }
    }

    @Override
    public List<Record> findRecords(Criteria criteria, Order order, int limit) {
        checkNotNull(criteria, "criteria");
        checkNotNull(order, "order");
        checkArgument(limit >= 1, "limit >= 1");

        try {
            List<Record> records = Lists.newArrayList();

            Cursor<org.jooq.Record> cursor = getRecordsCursor(criteria, order, limit);

            try {
                while (cursor.hasNext()) {
                    records.add(mapJooqRecord(cursor.fetchOne()));
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
                if (record.getUserId() != null) {
                    userIds.add(record.getUserId());
                }
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
                    builder.append(ctx.render(LOG.X)).append(", ");
                    builder.append(ctx.render(LOG.Y)).append(", ");
                    builder.append(ctx.render(LOG.Z)).append(", ");
                    builder.append(ctx.render(LOG.ACTION)).append(", ");
                    builder.append(ctx.render(LOG.DATA));
                    builder.append(") VALUES ");

                    boolean first = true;
                    for (Record record : partition) {
                        Integer userId = record.getUserId() != null ? resolvedUsers.get(record.getUserId()) : null;
                        Short worldId = worldIds.get(record.getLocation().getWorldName().toLowerCase());
                        WorldVector3i location = record.getLocation();

                        if (worldId == null) throw new IllegalStateException("World resolution failed for " + record.getLocation());

                        if (first) {
                            first = false;
                        } else {
                            builder.append(", ");
                        }
                        builder.append("(?, ?, ?, ?, ?, ?, ?, ?)");

                        values.add(new Timestamp(record.getTime().getTime()));
                        values.add(userId);
                        values.add(worldId);
                        values.add(location.getX());
                        values.add((short) location.getY());
                        values.add(location.getZ());
                        values.add(record.getAction());
                        values.add(record.getData());
                    }

                    create.execute(builder.toString(), values.toArray(new Object[values.size()]));
                }
            });
        } catch (org.jooq.exception.DataAccessException e) {
            throw new DataAccessException("Failed to save record", e);
        }
    }

    private class RecordCursor implements com.skcraft.plume.common.util.Cursor<Record> {
        private final Cursor<org.jooq.Record> cursor;

        public RecordCursor(Cursor<org.jooq.Record> cursor) {
            this.cursor = cursor;
        }

        @Override
        public boolean hasNext() {
            return cursor.hasNext();
        }

        @Override
        public Record next() {
            return mapJooqRecord(cursor.fetchOne());
        }

        @Override
        public void close() throws IOException {
            cursor.close();
        }
    }
}
