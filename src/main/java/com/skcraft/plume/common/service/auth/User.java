package com.skcraft.plume.common.service.auth;

import com.google.common.collect.Sets;
import com.skcraft.plume.common.UserId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Set;

/**
 * A user entry.
 */
@Data
@EqualsAndHashCode(of = "userId")
@ToString(exclude = {"hostKey", "groups", "subject"})
public class User {

    private UserId userId;
    @Nullable private UserId referrer;
    @Nullable private Date createDate;
    @Nullable private Date joinDate;
    @Nullable private Date lastOnline;
    @Nullable private String hostKey;
    private transient Set<Group> groups = Sets.newConcurrentHashSet();
    private transient Subject subject = new UserSubject(this);

    public void refresh() {
        subject = new UserSubject(this);
    }

}
