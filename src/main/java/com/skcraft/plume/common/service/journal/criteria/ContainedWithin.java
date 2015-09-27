package com.skcraft.plume.common.service.journal.criteria;

import com.sk89q.intake.argument.ArgumentException;
import com.sk89q.intake.argument.ArgumentParseException;
import com.sk89q.intake.argument.CommandArgs;
import com.sk89q.intake.argument.Namespace;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.Region;
import com.skcraft.plume.common.util.WorldVector3i;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jooq.Condition;

import static com.skcraft.plume.common.service.sql.model.log.tables.Log.LOG;
import static com.skcraft.plume.common.service.sql.model.log.tables.LogWorld.LOG_WORLD;
import static com.skcraft.plume.common.util.SharedLocale.tr;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContainedWithin implements Criteria {

    private String world;
    private Region region;

    @Override
    public Condition toCondition() {
        Vector min = region.getMinimumPoint();
        Vector max = region.getMaximumPoint();
        Condition condition = LOG_WORLD.NAME.eq(world);
        condition = condition.and(LOG.X.greaterOrEqual(min.getBlockX()));
        condition = condition.and(LOG.Y.greaterOrEqual((short) min.getBlockY()));
        condition = condition.and(LOG.Z.greaterOrEqual(min.getBlockZ()));
        condition = condition.and(LOG.X.lessOrEqual(max.getBlockX()));
        condition = condition.and(LOG.Y.lessOrEqual((short) max.getBlockY()));
        condition = condition.and(LOG.Z.lessOrEqual(max.getBlockZ()));
        return condition;
    }

    @Override
    public void parse(CommandArgs args, Namespace namespace) throws ArgumentException {
        Region region = (Region) namespace.get("selection");
        WorldVector3i center = (WorldVector3i) namespace.get("center");
        if (region != null) {
            if (center != null) {
                this.world = center.getWorldId();
            } else {
                throw new ArgumentParseException(tr("logger.criteriaParser.noCenter"));
            }
            this.region = region;
        } else {
            throw new ArgumentParseException(tr("logger.criteriaParser.noSelectedArea"));
        }
    }
}
