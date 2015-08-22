package com.skcraft.plume.common.service.party;

import com.skcraft.plume.common.UserId;

import static com.skcraft.plume.common.util.SharedLocale.tr;

public final class Parties {

    private Parties() { }

    public static boolean canManage(Party party, UserId userId) {
        for (Member member : party.getMembers()) {
            if (member.getUserId().equals(userId)) {
                return member.getRank().canManage();
            }
        }

        return false;
    }

    public static Member getMemberByUser(Party party, UserId user) {
        for (Member member : party.getMembers()) {
            if (member.getUserId().equals(user)) {
                return member;
            }
        }

        return null;
    }

    public static String getMemberListStr(Party party) {
        StringBuilder str = new StringBuilder();
        str.append("§eMembers: ");

        for (Member member : party.getMembers()) {
            switch(member.getRank()) {
                case OWNER:
                    str.append("§1").append(member.getUserId().getName()).append("§e, ");
                    break;

                case MANAGER:
                    str.append("§9").append(member.getUserId().getName()).append("§e, ");
                    break;

                case MEMBER:
                    str.append("§b").append(member.getUserId().getName()).append("§e, ");
                    break;
            }
        }

        return str.toString();
    }

/*
    public static String getUserMembersOfStr(UserId user) {
        StringBuilder str = new StringBuilder();
        str.append("§eYou're a part of: ");

        str.append("§4!!!UNIMPLEMENTED!!!");

        for (Member member : party.getMembers()) {
            switch(member.getRank()) {
                case OWNER:
                    str.append("§1").append(member.getUserId().getName()).append("§e, ")
                    break;

                case MANAGER:
                    str.append("§9").append(member.getUserId().getName()).append("§e, ")
                    break;

                case MEMBER:
                    str.append("§b").append(member.getUserId().getName()).append("§e, ")
                    break;
            }
        }

        return str.toString();
    }
*/
}