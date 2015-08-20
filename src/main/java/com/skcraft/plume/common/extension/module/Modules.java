package com.skcraft.plume.common.extension.module;

public final class Modules {

    private Modules() {
    }

    public static String getModuleName(Class<?> type) {
        Module module = type.getAnnotation(Module.class);
        if (module != null) {
            return module.name();
        } else {
            return type.getName();
        }
    }

}
