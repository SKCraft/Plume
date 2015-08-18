package com.skcraft.plume.common.journal;

import com.sk89q.worldedit.regions.Region;
import com.skcraft.plume.common.UserId;
import lombok.Data;

import javax.annotation.Nullable;
import java.util.Date;

/**
 * A list of optional criteria that can be used to filter the range of
 * records retrieved.
 */
@Data
public final class Criteria {

    @Nullable private final String worldName;
    @Nullable private final Region containedWith;
    @Nullable private final Date since;
    @Nullable private final Date before;
    @Nullable private final UserId userId;

    public static class Builder {
        @Nullable private String worldName;
        @Nullable private Region containedWith;
        @Nullable private Date since;
        @Nullable private Date before;
        @Nullable private UserId userId;

        @Nullable
        public String getWorldName() {
            return worldName;
        }

        public Builder setWorldName(@Nullable String worldName) {
            this.worldName = worldName;
            return this;
        }

        @Nullable
        public Region getContainedWith() {
            return containedWith;
        }

        public Builder setContainedWith(@Nullable Region containedWith) {
            this.containedWith = containedWith;
            return this;
        }

        @Nullable
        public Date getSince() {
            return since;
        }

        public Builder setSince(@Nullable Date since) {
            this.since = since;
            return this;
        }

        @Nullable
        public Date getBefore() {
            return before;
        }

        public Builder setBefore(@Nullable Date before) {
            this.before = before;
            return this;
        }

        @Nullable
        public UserId getUserId() {
            return userId;
        }

        public Builder setUserId(@Nullable UserId userId) {
            this.userId = userId;
            return this;
        }

        public Criteria build() {
            return new Criteria(worldName, containedWith, since, before, userId);
        }
    }

}
