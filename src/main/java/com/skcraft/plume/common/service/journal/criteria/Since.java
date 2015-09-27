package com.skcraft.plume.common.service.journal.criteria;

import com.google.inject.Inject;
import com.sk89q.intake.argument.ArgumentException;
import com.sk89q.intake.argument.CommandArgs;
import com.sk89q.intake.argument.Namespace;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.jooq.Condition;

import java.sql.Timestamp;
import java.util.Date;

import static com.skcraft.plume.common.service.sql.model.log.tables.Log.LOG;

@Data
@NoArgsConstructor
public class Since implements Criteria {

    @Inject
    private PeriodFormatter periodFormatter;
    private Date after;

    public Since(Date after) {
        this.after = after;
    }

    @Override
    public Condition toCondition() {
        return LOG.TIME.greaterThan(new Timestamp(after.getTime()));
    }

    @Override
    public void parse(CommandArgs args, Namespace namespace) throws ArgumentException {
        Period p = periodFormatter.parsePeriod(args.next());
        after = new DateTime().minus(p).toDate();
    }

    @Override
    public boolean hasSpecificCriteria() {
        return true;
    }

}
