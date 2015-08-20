package com.skcraft.plume.common.auth;

import com.skcraft.plume.common.UserId;

public interface Authorizer {

    Subject getSubject(UserId userId);

}
