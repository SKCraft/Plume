package com.skcraft.plume.common.service.sql;

import com.google.common.collect.Lists;
import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.ban.Ban;
import com.skcraft.plume.common.service.ban.BanManager;
import com.skcraft.plume.common.service.sql.model.data.tables.records.BanRecord;
import lombok.Getter;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;

import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.skcraft.plume.common.service.sql.model.data.tables.Ban.BAN;
import static com.skcraft.plume.common.service.sql.model.data.tables.UserId.USER_ID;

public class DatabaseBans implements BanManager {

    private static final Condition ACTIVE_BAN_CONDITION = BAN.EXPIRE_TIME.isNull().or(BAN.EXPIRE_TIME.gt(Timestamp.from(Instant.now())));
    @Getter
    private final DatabaseManager database;

    public DatabaseBans(DatabaseManager database) {
        checkNotNull(database, "database");
        this.database = database;
    }

    @Override
    public List<Ban> findActiveBans(UserId id) throws DataAccessException {
        checkNotNull(id, "id");

        try {
            DSLContext create = database.create();

            List<Ban> bans = Lists.newArrayList();

            // Select user IDs, join bans
            Result<Record> banRecords = create
                    .select(BAN.fields())
                    .select(USER_ID.fields())
                    .from(USER_ID)
                    .crossJoin(BAN)
                    .where(USER_ID.UUID.eq(id.getUuid().toString()).and(BAN.USER_ID.eq(USER_ID.ID)).and(ACTIVE_BAN_CONDITION))
                    .fetch();

            for (Record record : banRecords) {
                Ban ban = database.getModelMapper().map(record, Ban.class);
                UserId userId = new UserId(UUID.fromString(record.getValue(USER_ID.UUID)), record.getValue(USER_ID.NAME));
                ban.setUserId(userId);
                bans.add(ban);
            }

            return bans;
        } catch (org.jooq.exception.DataAccessException e) {
            throw new DataAccessException("Failed to get active bans for " + id, e);
        }
    }

    @Override
    public int addBan(Ban ban) throws DataAccessException {
        checkNotNull(ban, "ban");
        try {
            DSLContext create = database.create();

            int userId = database.getUserIdCache().getOrCreateUserId(create, ban.getUserId());

            BanRecord record = create.newRecord(BAN, ban);
            //checkArgument(record.getId() == null, "Can't save existing ban");
            record.setId(null);
            record.setUserId(userId);

            BanRecord result = create
                    .insertInto(BAN)
                    .set(record)
                    .returning(BAN.ID)
                    .fetchOne();

            return result.getId();
        } catch (org.jooq.exception.DataAccessException e) {
            throw new DataAccessException("Failed to save ban", e);
        }
    }

    @Override
    public void pardon(UserId user, @Nullable UserId pardonUser, @Nullable String pardonReason) throws DataAccessException {
        checkNotNull(user, "user");

        Timestamp now = Timestamp.from(Instant.now());

        try {
            DSLContext create = database.create();

            UUID userUuid = checkNotNull(user.getUuid(), "user.getUuid()");
            Integer pardonUserId = pardonUser != null ? database.getUserIdCache().getOrCreateUserId(create, pardonUser) : null;

            create.update(BAN)
                    .set(BAN.EXPIRE_TIME, now)
                    .set(BAN.PARDON_BY, pardonUserId != null ? pardonUserId : null)
                    .set(BAN.PARDON_REASON, pardonReason)
                    .where(BAN.USER_ID.in(create.select(USER_ID.ID).from(USER_ID).where(USER_ID.UUID.eq(userUuid.toString()))).and(ACTIVE_BAN_CONDITION))
                    .execute();
        } catch (org.jooq.exception.DataAccessException e) {
            throw new DataAccessException("Failed to pardon bans for " + user, e);
        }
    }

}
