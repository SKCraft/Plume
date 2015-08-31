package com.skcraft.plume.common.service.claim;

import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.util.WorldVector3i;
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

    public Claim() {
    }

    public Claim(String server, String world, int x, int z) {
        this.server = server;
        this.world = world;
        this.x = x;
        this.z = z;
    }

    public Claim(String server, WorldVector3i position) {
        this.server = server;
        this.world = position.getWorldId();
        this.x = position.getX();
        this.z = position.getZ();
    }


}
