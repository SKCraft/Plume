package com.skcraft.plume.common.service.journal.criteria;

import com.sk89q.intake.argument.ArgumentException;
import com.sk89q.intake.argument.ArgumentParseException;
import com.sk89q.intake.argument.CommandArgs;
import com.sk89q.intake.argument.Namespace;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.skcraft.plume.common.util.Vectors;
import com.skcraft.plume.common.util.WorldVector3i;

import static com.skcraft.plume.common.util.SharedLocale.tr;

public class WithinRadius extends ContainedWithin {

    @Override
    public void parse(CommandArgs args, Namespace namespace) throws ArgumentException {
        int apothem;
        try {
            apothem = Integer.parseInt(args.next());
        } catch (NumberFormatException e) {
            throw new ArgumentParseException(tr("logger.criteriaParser.notIntegerRadius"));
        }

        WorldVector3i center = (WorldVector3i) namespace.get("center");

        if (center == null) {
            throw new ArgumentParseException(tr("logger.criteriaParser.noCenter"));
        }

        Vector min = Vectors.toVector(center).subtract(apothem, apothem, apothem);
        Vector max = Vectors.toVector(center).add(apothem, apothem, apothem);
        setWorld(center.getWorldId());
        setRegion(new CuboidRegion(min, max));
    }

}
