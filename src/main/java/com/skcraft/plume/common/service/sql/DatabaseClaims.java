package com.skcraft.plume.common.service.sql;

import com.google.common.collect.*;
import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.claim.Claim;
import com.skcraft.plume.common.service.claim.ClaimMap;
import com.skcraft.plume.common.util.WorldVector3i;
import org.jooq.*;
import org.jooq.impl.DSL;

import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.skcraft.plume.common.service.sql.model.data.tables.Claim.CLAIM;
import static com.skcraft.plume.common.service.sql.model.data.tables.UserId.USER_ID;
import static org.jooq.impl.DSL.row;

public class DatabaseClaims implements ClaimMap {

    /**
     * The number of rows to update at a time in one statement.
     */
    private static final int UPDATE_BATCH_SIZE = 100;
    private final DatabaseManager database;
    private final String server;

    public DatabaseClaims(DatabaseManager database, String server) {
        checkNotNull(database, "database");
        checkNotNull(server, "server");
        this.database = database;
        this.server = server;
    }

    @Nullable
    @Override
    public Claim findClaimByPosition(WorldVector3i position) {
        return findClaimsByPosition(Lists.newArrayList(position)).get(position);
    }

    @Override
    public Map<WorldVector3i, Claim> findClaimsByPosition(Collection<WorldVector3i> positions) {
        checkNotNull(positions, "positions");

        try {
            DSLContext create = database.create();

            Map<WorldVector3i, Claim> claims = Maps.newHashMap();

            // Collect Row3s (world, x, z) that will be used in the jOOQ query
            List<Row3<String, Integer, Integer>> coordRows = Lists.newArrayList();
            for (WorldVector3i position : positions) {
                coordRows.add(row(position.getWorldName(), position.getX(), position.getZ()));
            }

            Result<Record> claimRecords = create
                    .select(CLAIM.fields())
                    .select(USER_ID.fields())
                    .from(CLAIM)
                    .crossJoin(USER_ID)
                    .where(CLAIM.SERVER.eq(server)
                            .and(row(CLAIM.WORLD, CLAIM.X, CLAIM.Z).in(coordRows)
                                    .and(CLAIM.OWNER_ID.eq(USER_ID.ID))))
                    .fetch();

            for (Record record : claimRecords) {
                Claim claim = database.getModelMapper().map(record, Claim.class);
                UserId owner = new UserId(UUID.fromString(record.getValue(USER_ID.UUID)), record.getValue(USER_ID.NAME));
                claim.setOwner(owner);
                claims.put(new WorldVector3i(claim.getWorld(), claim.getX(), 0, claim.getZ()), claim);
            }

            return claims;
        } catch (org.jooq.exception.DataAccessException e) {
            throw new DataAccessException("Failed to fetch claims for given coordinates", e);
        }
    }

    @Override
    public List<Claim> saveClaim(Collection<WorldVector3i> positions, UserId owner, @Nullable String party) {
        checkNotNull(positions, "positions");
        checkNotNull(owner, "owner");

        try {
            return executeReplace(database.create(), positions, owner, party);
        } catch (org.jooq.exception.DataAccessException e) {
            throw new DataAccessException("Failed to fetch claims for given coordinates", e);
        }
    }

    private static List<Row3<String, Integer, Integer>> rowsFromPositions(Collection<WorldVector3i> positions) {
        List<Row3<String, Integer, Integer>> rows = Lists.newArrayList();
        for (WorldVector3i position : positions) {
            rows.add(row(position.getWorldName(), position.getX(), position.getZ()));
        }
        return rows;
    }

    @Override
    public void removeClaims(Collection<WorldVector3i> positions) {
        checkNotNull(positions, "positions");

        try {
            DSLContext create = database.create();

            // We will remove entries from this set
            Set<WorldVector3i> freePositions = Sets.newHashSet(positions);

            // Collect Row3s (world, x, z) that will be used in the jOOQ query
            List<Row3<String, Integer, Integer>> coordRows = rowsFromPositions(positions);

            create
                    .deleteFrom(CLAIM)
                    .where(CLAIM.SERVER.eq(server).and(row(CLAIM.WORLD, CLAIM.X, CLAIM.Z).in(coordRows)))
                    .execute();
        } catch (org.jooq.exception.DataAccessException e) {
            throw new DataAccessException("Failed to fetch claims for given coordinates", e);
        }
    }

    @Override
    public List<Claim> updateClaim(Collection<WorldVector3i> positions, UserId owner, @Nullable String party, @Nullable UserId existingOwner) {
        checkNotNull(positions, "positions");

        try {
            DSLContext create = database.create();

            // We will remove entries from this set
            Set<WorldVector3i> freePositions = Sets.newHashSet(positions);

            // Collect Row3s (world, x, z) that will be used in the jOOQ query
            List<Row3<String, Integer, Integer>> coordRows = rowsFromPositions(positions);

            Cursor<Record> claimRecords = create
                    .select(CLAIM.fields())
                    .select(USER_ID.fields())
                    .from(CLAIM)
                    .crossJoin(USER_ID)
                    .where(CLAIM.SERVER.eq(server)
                            .and(row(CLAIM.WORLD, CLAIM.X, CLAIM.Z).in(coordRows)
                                    .and(CLAIM.OWNER_ID.eq(USER_ID.ID))))
                    .fetchLazy();

            try {
                for (Record record : claimRecords) {
                    UUID uuid = UUID.fromString(record.getValue(USER_ID.UUID));
                    if (existingOwner == null || !uuid.equals(existingOwner.getUuid())) {
                        freePositions.remove(new WorldVector3i(record.getValue(CLAIM.WORLD), record.getValue(CLAIM.X), 0, record.getValue(CLAIM.Z)));
                    }
                }
            } finally {
                claimRecords.close();
            }

            // Since we are not locking the rows, this operation is not atomic,
            // so only one thread may update claims at a time
            return executeReplace(create, freePositions, owner, party);
        } catch (org.jooq.exception.DataAccessException e) {
            throw new DataAccessException("Failed to fetch claims for given coordinates", e);
        }
    }

    @Override
    public int getClaimCount(UserId owner) {
        checkNotNull(owner, "owner");

        try {
            DSLContext create = database.create();

            int ownerId = database.getUserIdCache().getOrCreateUserId(create, owner);

            Record record = create.selectCount()
                    .from(CLAIM)
                    .where(CLAIM.SERVER.eq(server).and(CLAIM.OWNER_ID.eq(ownerId)))
                    .fetchOne();

            return (Integer) record.getValue(0);
        } catch (org.jooq.exception.DataAccessException e) {
            throw new DataAccessException("Failed to fetch the number of owned claims", e);
        }
    }

    private List<Claim> executeReplace(DSLContext context, Collection<WorldVector3i> positions, UserId owner, @Nullable String party) {
        checkNotNull(positions, "positions");
        checkNotNull(owner, "owner");

        List<Claim> claims = Lists.newArrayList();

        context.transaction(configuration -> {
            DSLContext create = DSL.using(configuration);
            RenderContext ctx = create.renderContext().qualify(false);

            int ownerId = database.getUserIdCache().getOrCreateUserId(create, owner);
            java.sql.Date now = new java.sql.Date(new Date().getTime());

            UnmodifiableIterator<List<WorldVector3i>> it = Iterators.partition(positions.iterator(), UPDATE_BATCH_SIZE);
            while (it.hasNext()) {
                List<Object> values = Lists.newArrayList();

                StringBuilder builder = new StringBuilder();
                builder.append("REPLACE INTO ");
                builder.append(create.render(CLAIM));
                builder.append(" (");
                builder.append(ctx.render(CLAIM.SERVER)).append(", ");
                builder.append(ctx.render(CLAIM.WORLD)).append(", ");
                builder.append(ctx.render(CLAIM.X)).append(", ");
                builder.append(ctx.render(CLAIM.Z)).append(", ");
                builder.append(ctx.render(CLAIM.OWNER_ID)).append(", ");
                builder.append(ctx.render(CLAIM.PARTY_NAME)).append(", ");
                builder.append(ctx.render(CLAIM.ISSUE_TIME));
                builder.append(") VALUES ");

                boolean first = true;
                for (WorldVector3i position : it.next()) {
                    if (first) {
                        first = false;
                    } else {
                        builder.append(", ");
                    }
                    builder.append("(?, ?, ?, ?, ?, ?, ?)");

                    values.add(server);
                    values.add(position.getWorldName());
                    values.add(position.getX());
                    values.add(position.getZ());
                    values.add(ownerId);
                    values.add(party);
                    values.add(now);

                    Claim claim = new Claim();
                    claim.setServer(server);
                    claim.setWorld(position.getWorldName());
                    claim.setX(position.getX());
                    claim.setZ(position.getZ());
                    claim.setOwner(owner);
                    claim.setParty(party);
                    claim.setIssueTime(now);
                    claims.add(claim);
                }

                create.execute(builder.toString(), values.toArray(new Object[values.size()]));
            }
        });

        return claims;
    }

}
