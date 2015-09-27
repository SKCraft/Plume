package com.skcraft.plume.common.service.journal.criteria;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.sk89q.intake.argument.ArgumentException;
import com.sk89q.intake.argument.ArgumentParseException;
import com.sk89q.intake.argument.CommandArgs;
import com.sk89q.intake.argument.Namespace;
import com.skcraft.plume.module.backtrack.ActionMap;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jooq.Condition;

import java.util.List;

import static com.skcraft.plume.common.service.sql.model.log.tables.Log.LOG;
import static com.skcraft.plume.common.util.SharedLocale.tr;

@Data
@NoArgsConstructor
public class TypeOf implements Criteria {

    @Inject
    private ActionMap actionMap;
    private List<Short> actions;

    public TypeOf(List<Short> actions) {
        this.actions = actions;
    }

    @Override
    public Condition toCondition() {
        return LOG.ACTION.in(actions);
    }

    @Override
    public void parse(CommandArgs args, Namespace namespace) throws ArgumentException {
        String value = args.next();

        if (value.trim().isEmpty()) {
            throw new ArgumentParseException(tr("logger.criteriaParser.emptyActionArgument"));
        }
        List<Short> actions = Lists.newArrayList();
        for (String token : value.split(",")) {
            Short id = actionMap.getActionIdByName(token);
            if (id != null) {
                actions.add(id);
            } else {
                throw new ArgumentParseException(tr("logger.criteriaParser.unknownAction", token));
            }
        }
        this.actions = actions;
    }
}
