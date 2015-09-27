package com.skcraft.plume.common.service.journal.criteria;

import com.sk89q.intake.argument.ArgumentException;
import com.sk89q.intake.argument.CommandArgs;
import com.sk89q.intake.argument.Namespace;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jooq.Condition;

import static org.jooq.impl.DSL.not;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Not implements Criteria {

    private Criteria criteria;

    @Override
    public Condition toCondition() {
        return not(criteria.toCondition());
    }

    @Override
    public void parse(CommandArgs args, Namespace namespace) throws ArgumentException {
    }

}
