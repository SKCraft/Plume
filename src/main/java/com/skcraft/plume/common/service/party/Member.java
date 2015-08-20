package com.skcraft.plume.common.service.party;

import com.skcraft.plume.common.UserId;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a member (with associated user and rank) in a party.
 */
@Data
@EqualsAndHashCode(of = "userId")
public class Member {

    private UserId userId;
    private Rank rank;

    public Member() {
    }

    public Member(UserId userId, Rank rank) {
        this.userId = userId;
        this.rank = rank;
    }

}
