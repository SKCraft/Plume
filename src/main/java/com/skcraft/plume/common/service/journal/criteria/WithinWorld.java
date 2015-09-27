package com.skcraft.plume.common.service.journal.criteria;

import com.sk89q.intake.argument.ArgumentException;
import com.sk89q.intake.argument.CommandArgs;
import com.sk89q.intake.argument.Namespace;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jooq.Condition;

import static com.skcraft.plume.common.service.sql.model.log.tables.LogWorld.LOG_WORLD;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithinWorld implements Criteria {

    private String world;

    @Override
    public Condition toCondition() {
        return LOG_WORLD.NAME.eq(world);
    }

    @Override
    public void parse(CommandArgs args, Namespace namespace) throws ArgumentException {
        world = args.next();
    }

}
