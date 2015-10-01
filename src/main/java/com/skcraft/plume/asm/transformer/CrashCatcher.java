package com.skcraft.plume.asm.transformer;

import com.skcraft.plume.asm.util.MethodMatcher;
import lombok.Getter;

@Getter
public class CrashCatcher {

    private final MethodMatcher methodMatcher;
    private final MethodMatcher invokeMatcher;

    public CrashCatcher(MethodMatcher methodMatcher, MethodMatcher invokeMatcher) {
        this.methodMatcher = methodMatcher;
        this.invokeMatcher = invokeMatcher;
    }

}
