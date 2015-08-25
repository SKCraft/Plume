package com.skcraft.plume.common.service.party;

import com.skcraft.plume.common.UserId;

import static com.skcraft.plume.common.util.SharedLocale.tr;

public final class Parties {

    private Parties() { }

    public static boolean isMember(Party party, UserId userId) {
        return party.getMembers().contains(new Member(userId, Rank.MEMBER));
    }

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
        boolean isFirst = true;

        str.append(tr("party.info.memberlist")).append(" ");

        for (Member member : party.getMembers()) {
            switch(member.getRank()) {
                    case OWNER:
                    if (!isFirst) str.append(", ");
                    str.append(tr("party.info.colors.owner"))
                            .append(member.getUserId().getName())
                            .append(tr("party.info.colors.sep"));
                    break;

                case MANAGER:
                    if (!isFirst) str.append(", ");
                    str.append(tr("party.info.colors.manager"))
                            .append(member.getUserId().getName())
                            .append(tr("party.info.colors.sep"));
                    break;

                case MEMBER:
                    if (!isFirst) str.append(", ");
                    str.append(tr("party.info.colors.member"))
                            .append(member.getUserId().getName())
                            .append(tr("party.info.colors.sep"));
                    break;
            }
            if (isFirst) isFirst = false;
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
