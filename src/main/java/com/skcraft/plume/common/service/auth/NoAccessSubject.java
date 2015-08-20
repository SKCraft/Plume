package com.skcraft.plume.common.service.auth;

public final class NoAccessSubject implements Subject {

    public static final NoAccessSubject INSTANCE = new NoAccessSubject();

    private NoAccessSubject() {
    }

    @Override
    public boolean hasPermission(String permission, Context context) {
        return false;
    }

}
