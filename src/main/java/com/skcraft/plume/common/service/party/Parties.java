package com.skcraft.plume.common.service.party;

import com.skcraft.plume.common.UserId;

public final class Parties {

    private Parties() {
    }

    public static boolean canManage(Party party, UserId userId) {
        for (Member member : party.getMembers()) {
            if (member.getUserId() == userId) {
                return member.getRank().canManage();
            }
        }

        return false;
    }

}