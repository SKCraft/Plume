package com.skcraft.plume.common.claim;

import com.skcraft.plume.common.UserId;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * Represents a chunk that can be claimed by a user.
 */
@Data
@EqualsAndHashCode(of = {"server", "world", "x", "z"})
public class Claim {

    private String server;
    private String world;
    private int x;
    private int z;
    private UserId owner;
    private String party;
    private Date issueTime;

}
