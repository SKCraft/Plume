package com.skcraft.plume.common.util;

import com.google.common.base.Function;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringInterpolation {

    private StringInterpolation() {
    }

    public static String interpolate(Pattern pattern, String text, Collection<Function<String, String>> resolvers) {
        return interpolate(pattern, text, s -> {
            for (Function<String, String> function : resolvers) {
                String value = function.apply(s);
                if (value != null) {
                    return value;
                }
            }
            return null;
        });
    }

    public static String interpolate(Pattern pattern, String text, Function<String, String> resolver) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = resolver.apply(matcher.group(1));
            if (replacement != null) {
                matcher.appendReplacement(buffer, "");
                buffer.append(replacement);
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

}
