package com.skcraft.plume.common.util;

import java.util.Calendar;
import java.util.regex.Pattern;

public class PathnameBuilder {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("%([^%])");

    private final Calendar calendar = Calendar.getInstance();

    public String interpolate(String filename) {
        return StringInterpolation.interpolate(VARIABLE_PATTERN, filename, input -> {
            switch (input) {
                case "Y": return String.format("%04d", calendar.get(Calendar.YEAR));
                case "m": return String.format("%02d", calendar.get(Calendar.MONTH));
                case "d": return String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH));
                case "W": return String.format("%02d", calendar.get(Calendar.WEEK_OF_YEAR));
                case "H": return String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY));
                case "h": return String.format("%02d", calendar.get(Calendar.HOUR));
                case "i": return String.format("%02d", calendar.get(Calendar.MINUTE));
                case "s": return String.format("%02d", calendar.get(Calendar.SECOND));
                default: return null;
            }
        });
    }

}
