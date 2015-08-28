package com.skcraft.plume.service;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.event.lifecycle.InitializationEvent;
import com.skcraft.plume.common.service.auth.*;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.common.util.service.InjectService;
import com.skcraft.plume.common.util.service.Service;
import com.skcraft.plume.common.util.service.ServiceLocator;
import com.skcraft.plume.util.Server;
import ninja.leaping.configurate.objectmapping.Setting;

@Module(name = "default-authorizer")
public class DefaultAuthorizer implements Authorizer {

    @Inject
    private ServiceLocator services;
    @InjectService(required = false)
    private Service<UserCache> userCache;
    @InjectConfig("default_authorizer")
    private Config<AuthorizerConfig> config;

    @Subscribe
    public void onInitializationEvent(InitializationEvent event) {
        services.register(Authorizer.class, this);
    }

    @Override
    public Subject getSubject(UserId userId) {
        if (config.get().grantOpAllPrivileges && Server.isOp(userId)) {
            return Subjects.permissive();
        }

        Optional<UserCache> userCache = this.userCache.get();
        if (userCache.isPresent()) {
            User user = userCache.get().getIfPresent(userId);
            if (user != null) {
                return user.getSubject();
            } else {
                return NoAccessSubject.INSTANCE;
            }
        } else {
            return Subjects.restrictive();
        }
    }

    private static class AuthorizerConfig {
        @Setting(comment = "Set to true to give all operators all privileges")
        public boolean grantOpAllPrivileges = true;
    }

}
