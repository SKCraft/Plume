package com.skcraft.plume.common.service.journal.criteria;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.sk89q.intake.argument.ArgumentException;
import com.sk89q.intake.argument.ArgumentParseException;
import com.sk89q.intake.argument.MutableStringListArgs;
import com.sk89q.intake.argument.Namespace;
import lombok.extern.java.Log;

import java.util.List;
import java.util.Map;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Log
public class CriteriaParser {

    private final Injector injector;
    private final Map<String, Class<? extends Criteria>> criteriaTypes = Maps.newHashMap();

    @Inject
    public CriteriaParser(Injector injector) {
        this.injector = injector;

        criteriaTypes.put("world", WithinWorld.class);
        criteriaTypes.put("w", WithinWorld.class);
        criteriaTypes.put("user", CausedBy.class);
        criteriaTypes.put("player", CausedBy.class);
        criteriaTypes.put("p", CausedBy.class);
        criteriaTypes.put("before", PriorTo.class);
        criteriaTypes.put("b", PriorTo.class);
        criteriaTypes.put("since", Since.class);
        criteriaTypes.put("after", Since.class);
        criteriaTypes.put("time", Since.class);
        criteriaTypes.put("t", Since.class);
        criteriaTypes.put("radius", WithinRadius.class);
        criteriaTypes.put("r", WithinRadius.class);
        criteriaTypes.put("region", ContainedWithin.class);
        criteriaTypes.put("action", TypeOf.class);
        criteriaTypes.put("a", TypeOf.class);
    }

    public Criteria parse(String input, Namespace namespace) throws ArgumentException {
        List<Criteria> criteria = Lists.newArrayList();
        MutableStringListArgs arguments = new MutableStringListArgs(Lists.newArrayList(input.split(" +")), Maps.newHashMap(), namespace);

        while (arguments.hasNext()) {
            String argument = arguments.next();
            boolean negated = false;

            if (argument.startsWith("!") || argument.startsWith("-")) {
                argument = argument.substring(1);
                negated = true;
            }

            if (argument.length() >= 2 && argument.charAt(1) == ':') {
                int index = argument.indexOf(':');
                arguments.insert(argument.substring(index + 1));
                argument = argument.substring(0, index);
            }

            Class<? extends Criteria> criteriaClass = criteriaTypes.get(argument.toLowerCase());
            if (criteriaClass != null) {
                Criteria criterion = injector.getInstance(criteriaClass);
                criterion.parse(arguments, namespace);
                criteria.add(negated ? new Not(criterion) : criterion);
            } else {
                throw new ArgumentParseException(tr("logger.criteriaParser.noSuchParameter", argument));
            }
        }

        if (criteria.isEmpty()) {
            return AlwaysTrue.INSTANCE;
        } else {
            return new And(criteria);
        }
    }

}
