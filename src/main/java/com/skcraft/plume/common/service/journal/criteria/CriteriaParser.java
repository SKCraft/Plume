package com.skcraft.plume.common.service.journal.criteria;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.journal.criteria.Criteria.Builder;
import com.skcraft.plume.common.util.Vectors;
import com.skcraft.plume.common.util.WorldVector3i;
import com.skcraft.plume.util.profile.ProfileLookupException;
import com.skcraft.plume.util.profile.ProfileNotFoundException;
import com.skcraft.plume.util.profile.ProfileService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.skcraft.plume.common.util.SharedLocale.tr;

@Log
public class CriteriaParser {

    private static final PeriodFormatter PERIOD_FORMATTER = new PeriodFormatterBuilder()
            .appendYears().appendSuffix("y")
            .appendMonths().appendSuffix("mt")
            .appendWeeks().appendSuffix("w")
            .appendDays().appendSuffix("d")
            .appendHours().appendSuffix("h")
            .appendMinutes().appendSuffix("m")
            .appendSeconds().appendSuffix("s")
            .toFormatter();

    private final ProfileService profileService;
    private final Function<String, Short> actionToId;
    @Getter @Setter @Nullable
    private Region selection;
    @Getter @Setter
    private WorldVector3i center;

    public CriteriaParser(ProfileService profileService, Function<String, Short> actionToId) {
        checkNotNull(profileService, "profileService");
        checkNotNull(actionToId, "actionToId");
        this.actionToId = actionToId;
        this.profileService = profileService;
    }

    public Builder parse(String input) throws ParseException {
        if (input.isEmpty()) {
            return new Builder();
        }

        Builder builder = new Builder();
        String[] tokens = input.split(" ");

        try {
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                String param;
                String arg;

                if (token.equalsIgnoreCase("selection")) {
                    setContainedWithin(builder);
                    continue;
                }

                if (token.length() >= 2 && token.contains(":")) {
                    String[] parts = token.split(":", 2);
                    param = parts[0];
                    arg = parts[1];
                } else {
                    param = token;
                    arg = tokens[++i];
                }

                switch (param) {
                    case "world":
                    case "w":
                        setWorldId(builder, arg);
                        break;
                    case "user":
                    case "player":
                    case "p":
                        setUserId(builder, arg);
                        break;
                    case "before":
                    case "b":
                        setBefore(builder, arg);
                        break;
                    case "since":
                    case "time":
                    case "t":
                        setSince(builder, arg);
                        break;
                    case "area":
                    case "radius":
                    case "r":
                        setRadius(builder, arg);
                        break;
                    case "action":
                    case "a":
                        setActions(builder, arg);
                        break;
                    case "-a":
                    case "-action":
                        setExcludeActions(builder, arg);
                        break;
                    default:
                        throw new ParseException(tr("logger.criteriaParser.noSuchParameter", tokens[i]));
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ParseException(tr("logger.criteriaParser.missingLastArgument"));
        }

        return builder;
    }

    public List<Short> parseActions(String value) throws ParseException {
        if (value.trim().isEmpty()) {
            throw new ParseException(tr("logger.criteriaParser.emptyActionArgument"));
        }
        List<Short> actions = Lists.newArrayList();
        for (String token : value.split(",")) {
            Short id = actionToId.apply(token);
            if (id != null) {
                actions.add(id);
            } else {
                throw new ParseException(tr("logger.criteriaParser.unknownAction", token));
            }
        }
        return actions;
    }

    private void setActions(Builder builder, String value) throws ParseException {
        builder.setActions(parseActions(value));
    }

    private void setExcludeActions(Builder builder, String value) throws ParseException {
        builder.setExcludeActions(parseActions(value));
    }

    private Date parseDate(String value) throws ParseException {
        try {
            Period p = PERIOD_FORMATTER.parsePeriod(value);
            return new DateTime().minus(p).toDate();
        } catch (IllegalArgumentException e) {
            throw new ParseException("Date is not in an recognizable format: " + value);
        }
    }

    private void setBefore(Builder builder, String value) throws ParseException {
        builder.setBefore(parseDate(value));
    }

    private void setSince(Builder builder, String value) throws ParseException {
        builder.setSince(parseDate(value));
    }

    private void setRadius(Builder builder, String value) throws ParseException {
        int apothem;
        try {
            apothem = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ParseException(tr("logger.criteriaParser.notIntegerRadius"));
        }
        if (center == null) {
            throw new ParseException(tr("logger.criteriaParser.noCenter"));
        }
        Vector min = Vectors.toVector(center).subtract(apothem, apothem, apothem);
        Vector max = Vectors.toVector(center).add(apothem, apothem, apothem);
        builder.setContainedWithin(new CuboidRegion(min, max));
        builder.setWorldId(center.getWorldName());
    }

    private void setContainedWithin(Builder builder) throws ParseException {
        if (selection == null) {
            throw new ParseException(tr("logger.criteriaParser.noSelectedArea"));
        }
        builder.setContainedWithin(selection);
        if (center != null) {
            builder.setWorldId(center.getWorldName());
        }
    }

    private void setWorldId(Builder builder, String value) throws ParseException {
        builder.setWorldId(value);
    }

    private void setUserId(Builder builder, String value) throws ParseException {
        if (builder.getUserId() != null) {
            throw new ParseException(tr("logger.criteriaParser.playerAlreadySet"));
        }
        try {
            UserId userId = profileService.findUserId(value);
            builder.setUserId(userId);
        } catch (ProfileLookupException e) {
            log.log(Level.WARNING, "Failed to lookup a player's profile", e);
            throw new ParseException(tr("args.minecraftUserLookupFailed", value));
        } catch (ProfileNotFoundException e) {
            throw new ParseException(tr("args.minecraftUserNotFound", value));
        }
    }

    public static class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }
    }

}
