package com.skcraft.plume.common.service.party;

/**
 * The rank of a member in a party.
 */
public enum Rank {

    MEMBER(false),
    MANAGER(true),
    OWNER(true);

    private final boolean manage;

    Rank(boolean manage) {
        this.manage = manage;
    }

    public boolean canManage() {
        return manage;
    }

}