package com.skcraft.plume.common.service.sql;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.google.inject.Inject;
import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.module.MySQLPool;
import com.skcraft.plume.common.service.journal.Journal;
import com.skcraft.plume.common.service.journal.Record;
import com.skcraft.plume.common.service.journal.criteria.Criteria;
import com.skcraft.plume.common.service.sql.model.log.tables.records.LogWorldRecord;
import com.skcraft.plume.common.util.Order;
import com.skcraft.plume.common.util.WorldVector3i;
import lombok.Getter;
import lombok.extern.java.Log;
import org.jooq.Condition;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep1;
import org.jooq.RenderContext;
import org.jooq.SelectLimitStep;
import org.jooq.impl.DSL;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.skcraft.plume.common.service.sql.model.data.tables.UserId.USER_ID;
import static com.skcraft.plume.common.service.sql.model.log.tables.Log.LOG;
import static com.skcraft.plume.common.service.sql.model.log.tables.LogWorld.LOG_WORLD;

@Log
public class DatabaseJournal implements Journal {

    private static final int DATA_MAX_SIZE = 16777215;
    private static final int UPDATE_BATCH_SIZE = 100;
    private static final int MIN_Y = 0;
    private static final int MAX_Y = 255;
    private ConcurrentMap<String, Short> worldIds = Maps.newConcurrentMap();
    @Getter private final Supplier<DatabaseManager> database;

    @Inject
    public DatabaseJournal(MySQLPool pool) {
        this.database = pool::getDatabase;
    }

    /**
     * Create a new instance.
     *
     * @param database The database
     */
    public DatabaseJournal(DatabaseManager database) {
        checkNotNull(database, "database");
        this.database = () -> database;
    }

    @Override
    public void load() {
        ConcurrentMap<String, Short> worldIds = Maps.newConcurrentMap();
        List<LogWorldRecord> results = database.get().create().selectFrom(LOG_WORLD).fetch();
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

    private Record mapJooqRecord(org.jooq.Record result) {
        if (result == null) {
            return null;
        }
        Record record = new Record();
        record.setId(result.getValue(LOG.ID));
        record.setLocation(new WorldVector3i(result.getValue(LOG_WORLD.NAME), result.getValue(LOG.X), result.getValue(LOG.Y), result.getValue(LOG.Z)));
        record.setTime(result.getValue(LOG.TIME));
        record.setUserId(database.get().getUserIdCache().fromRecord(result, USER_ID));
        record.setAction(result.getValue(LOG.ACTION));
        record.setData(result.getValue(LOG.DATA));
        return record;
    }

    public Cursor<org.jooq.Record> getRecordsCursor(Criteria criteria, Order order, int limit) {
        checkNotNull(criteria, "criteria");
        checkNotNull(order, "order");

        try {
            DSLContext create = database.get().create();

            Condition condition = LOG.WORLD_ID.eq(LOG_WORLD.ID).and(criteria.toCondition());

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
            DSLContext context = database.get().create();

            // Collect user IDs and world IDs for separate insertion
            for (Record record : records) {
                if (record.getUserId() != null) {
                    userIds.add(record.getUserId());
                }
                worlds.add(record.getLocation().getWorldId().toLowerCase());
            }

            // Insert user IDs
            resolvedUsers = database.get().getUserIdCache().getOrCreateUserIds(context, userIds);

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
                        Short worldId = worldIds.get(record.getLocation().getWorldId().toLowerCase());
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
                        values.add(Math.max(Math.min((short) location.getY(), MAX_Y), MIN_Y));
                        values.add(location.getZ());
                        values.add(record.getAction());
                        values.add(record.getData().length <= DATA_MAX_SIZE ? record.getData() : null);
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
