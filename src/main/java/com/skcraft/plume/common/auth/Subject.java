package com.skcraft.plume.common.auth;

/**
 * An object that can have permissions assigned to it.
 */
public interface Subject {

    /**
     * Return whether this subject has the given permission.
     *
     * @param permission The permission
     * @return Whether the permission is granted
     */
    boolean hasPermission(String permission);

}
