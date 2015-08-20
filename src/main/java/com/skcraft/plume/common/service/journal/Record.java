package com.skcraft.plume.common.service.journal;

import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.util.WorldVector3i;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * A record keeps track of a change.
 */
@Data
@EqualsAndHashCode(of = "id")
public class Record {

    private int id;
    private UserId userId;
    private WorldVector3i location;
    private Action action;
    private Date time = new Date();

    public Record() {
    }

    public Record(int id, UserId userId, WorldVector3i location, Action action) {
        this.id = id;
        this.userId = userId;
        this.location = location;
        this.action = action;
    }

    public Record(int id, UserId userId, WorldVector3i location, Action action, Date time) {
        this.id = id;
        this.userId = userId;
        this.location = location;
        this.action = action;
        this.time = time;
    }
}
