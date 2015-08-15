package com.skcraft.plume.common.journal;

import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.util.WorldVector3i;

import java.util.Date;

/**
 * A record keeps track of a change.
 */
public class Record {

    private int id;
    private Date time;
    private UserId userId;
    private WorldVector3i location;
    private Action action;

}
