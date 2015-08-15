package com.skcraft.plume.common.journal;

import com.sk89q.worldedit.regions.Region;
import com.skcraft.plume.common.UserId;

import java.util.Date;

/**
 * A list of optional criteria that can be used to filter the range of
 * records retrieved.
 */
public class Criteria {

    private Region containedWith;
    private Date since;
    private Date before;
    private UserId userId;

}
