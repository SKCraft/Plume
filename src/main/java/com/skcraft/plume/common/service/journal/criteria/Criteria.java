package com.skcraft.plume.common.service.journal.criteria;

import com.sk89q.worldedit.regions.Region;
import com.skcraft.plume.common.UserId;
import lombok.Data;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

/**
 * A list of optional criteria that can be used to filter the range of
 * records retrieved.
 */
@Data
public final class Criteria {

    @Nullable private final String worldId;
    @Nullable private final Region containedWithin;
    @Nullable private final Date since;
    @Nullable private final Date before;
    @Nullable private final UserId userId;
    @Nullable private final List<Short> actions;
    @Nullable private final List<Short> excludeActions;

    public static class Builder {
        @Nullable private String worldId;
        @Nullable private Region containedWithin;
        @Nullable private Date since;
        @Nullable private Date before;
        @Nullable private UserId userId;
        @Nullable private List<Short> actions;
        @Nullable private List<Short> excludeActions;

        @Nullable
        public String getWorldId() {
            return worldId;
        }

        public Builder setWorldId(@Nullable String worldId) {
            this.worldId = worldId;
            return this;
        }

        @Nullable
        public Region getContainedWithin() {
            return containedWithin;
        }

        public Builder setContainedWithin(@Nullable Region containedWithin) {
            this.containedWithin = containedWithin;
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

        @Nullable
        public List<Short> getActions() {
            return actions;
        }

        public Builder setActions(@Nullable List<Short> actions) {
            this.actions = actions;
            return this;
        }

        @Nullable
        public List<Short> getExcludeActions() {
            return excludeActions;
        }

        public Builder setExcludeActions(@Nullable List<Short> excludeActions) {
            this.excludeActions = excludeActions;
            return this;
        }

        public Criteria build() {
            return new Criteria(worldId, containedWithin, since, before, userId, actions, excludeActions);
        }
    }

}
