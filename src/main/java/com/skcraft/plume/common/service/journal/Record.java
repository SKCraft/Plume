package com.skcraft.plume.common.service.journal;

import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.util.WorldVector3i;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * A record keeps track of a change.
 */
@Data
@EqualsAndHashCode(of = "id")
@ToString(exclude = "data")
public class Record implements Serializable {

    private static final long serialVersionUID = -4088435914925644178L;

    private int id;
    private UserId userId;
    private WorldVector3i location;
    private short action;
    private byte[] data;
    private Date time = new Date();

    public Record() {
    }

}
