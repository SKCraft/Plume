package com.skcraft.plume.common.service.journal.criteria;

import com.sk89q.intake.argument.ArgumentException;
import com.sk89q.intake.argument.CommandArgs;
import com.sk89q.intake.argument.Namespace;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jooq.Condition;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class And implements Criteria {

    private List<Criteria> criteria;

    @Override
    public Condition toCondition() {
        Condition condition = null;
        for (Criteria c : criteria) {
            if (condition == null) {
                condition = c.toCondition();
            } else {
                condition = condition.and(c.toCondition());
            }
        }
        return condition;
    }

    @Override
    public void parse(CommandArgs args, Namespace namespace) throws ArgumentException {
    }

    @Override
    public boolean hasSpecificCriteria() {
        for (Criteria c : criteria) {
            if (c.hasSpecificCriteria()) {
                return true;
            }
        }
        return false;
    }
}
