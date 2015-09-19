package com.skcraft.plume.service;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.auth.Authorizer;
import com.skcraft.plume.common.service.auth.NoAccessSubject;
import com.skcraft.plume.common.service.auth.Subject;
import com.skcraft.plume.common.service.auth.Subjects;
import com.skcraft.plume.common.service.auth.User;
import com.skcraft.plume.common.service.auth.UserCache;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.service.DefaultAuthorizer.InjectorModule;
import com.skcraft.plume.util.Server;
import ninja.leaping.configurate.objectmapping.Setting;

@Module(name = "default-authorizer",
        desc = "Lets other modules check permissions and optionally uses any loaded hive service",
        injectorModule = InjectorModule.class)
public class DefaultAuthorizer implements Authorizer {

    @Inject
    private UserCache userCache;
    @InjectConfig("default_authorizer")
    private Config<AuthorizerConfig> config;

    @Override
    public Subject getSubject(UserId userId) {
        if (config.get().grantOpAllPrivileges && Server.isOp(userId)) {
            return Subjects.permissive();
        }

        if (userCache != null) {
            User user = userCache.getIfPresent(userId);
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

    public static class InjectorModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(Authorizer.class).to(DefaultAuthorizer.class).in(Singleton.class);
        }
    }

}
