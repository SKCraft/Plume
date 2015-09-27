package com.skcraft.plume.common.service.journal.criteria;

import com.sk89q.intake.argument.ArgumentException;
import com.sk89q.intake.argument.CommandArgs;
import com.sk89q.intake.argument.Namespace;
import org.jooq.Condition;

public interface Criteria {

    Condition toCondition();

    void parse(CommandArgs args, Namespace namespace) throws ArgumentException;

    default boolean hasSpecificCriteria() {
        return false;
    }

}
