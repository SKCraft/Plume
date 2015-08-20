package com.skcraft.plume.common.service.ban;

import com.skcraft.plume.common.UserId;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.Nullable;
import java.util.Date;

/**
 * Represents a ban.
 */
@Data
@EqualsAndHashCode(of = "id")
public class Ban {

    private int id;
    private UserId userId;
    @Nullable private Date issueTime;
    @Nullable private UserId issueBy;
    @Nullable private String server;
    @Nullable private String reason;
    private boolean heuristic;
    @Nullable private Date expireTime;
    @Nullable private UserId pardonBy;
    @Nullable private String pardonReason;

}
