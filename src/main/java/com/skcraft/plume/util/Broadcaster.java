package com.skcraft.plume.util;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.skcraft.plume.common.service.auth.Authorizer;
import com.skcraft.plume.common.service.auth.Context;
import com.skcraft.plume.common.service.auth.Context.Builder;
import com.skcraft.plume.common.util.Environment;
import com.skcraft.plume.util.profile.Profiles;
import net.minecraft.util.IChatComponent;

@Singleton
public class Broadcaster {

    @Inject private Authorizer authorizer;
    @Inject private Environment environment;

    public void broadcast(IChatComponent message, String permission) {
        Context.Builder builder = new Builder();
        environment.update(builder);
        Context context = builder.build();
        Messages.broadcast(message, player -> authorizer.getSubject(Profiles.fromPlayer(player)).hasPermission(permission, context));
    }

}
