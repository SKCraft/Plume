package com.skcraft.plume.common.service.auth;

public final class Subjects {

    private static final PermissiveSubject PERMISSIVE = new PermissiveSubject();
    private static final RestrictiveSubject RESTRICTIVE = new RestrictiveSubject();

    private Subjects() {
    }

    public static Subject permissive() {
        return PERMISSIVE;
    }

    public static Subject restrictive() {
        return RESTRICTIVE;
    }

    private static class PermissiveSubject implements Subject {
        @Override
        public boolean hasPermission(String permission, Context context) {
            return true;
        }
    }

    private static class RestrictiveSubject implements Subject {
        @Override
        public boolean hasPermission(String permission, Context context) {
            return false;
        }
    }

}
