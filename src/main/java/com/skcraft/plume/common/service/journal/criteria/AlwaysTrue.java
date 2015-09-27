package com.skcraft.plume.common.service.journal.criteria;

import com.sk89q.intake.argument.ArgumentException;
import com.sk89q.intake.argument.CommandArgs;
import com.sk89q.intake.argument.Namespace;
import org.jooq.Condition;
import org.jooq.impl.DSL;

public class AlwaysTrue implements Criteria {

    public static final AlwaysTrue INSTANCE = new AlwaysTrue();

    private AlwaysTrue() {
    }

    @Override
    public Condition toCondition() {
        return DSL.trueCondition();
    }

    @Override
    public void parse(CommandArgs args, Namespace namespace) throws ArgumentException {
    }

}
