package com.skcraft.plume.module;

import com.google.inject.Inject;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.auth.Context;
import com.skcraft.plume.common.service.auth.User;
import com.skcraft.plume.common.service.auth.UserCache;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.service.InjectService;
import com.skcraft.plume.common.util.service.Service;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.common.util.Environment;
import com.skcraft.plume.event.network.PlayerAuthenticateEvent;
import com.skcraft.plume.util.profile.Profiles;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import ninja.leaping.configurate.objectmapping.Setting;

import java.util.Date;

@Module(name = "user-loader", hidden = true)
public class UserLoader {

    @InjectConfig("users")
    private Config<UsersConfig> config;
    @InjectService
    private Service<UserCache> userCache;
    @Inject
    private Environment environment;

    @SubscribeEvent
    public void onAuthenticate(PlayerAuthenticateEvent event) {
        UserCache userCache = this.userCache.provide();
        Date now = new Date();
        UserId userId = Profiles.fromProfile(event.getProfile());
        User user = userCache.getUser(userId, true);

        if (user != null) {
            if (user.getSubject().hasPermission("whitelist", environment.update(new Context.Builder()).build())) {
                user.setLastOnline(now);
                userCache.getHive().saveUser(user, false);
            } else {
                event.getNetHandler().func_147322_a(config.get().notWhitelistedMessage);
            }
        } else {
            // TODO: Allow user creation via config
            event.getNetHandler().func_147322_a(config.get().noUserMessage);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        UserCache userCache = this.userCache.provide();
        UserId userId = Profiles.fromPlayer(event.player);
        userCache.pin(userId); // Should already be loaded
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UserCache userCache = this.userCache.provide();
        UserId userId = Profiles.fromPlayer(event.player);
        userCache.unpin(userId);
    }

    private static class UsersConfig {
        @Setting(comment = "The message the user gets disconnected with if the user is not in the database")
        private String noUserMessage = "You are not whitelisted. Ask a friend to /invite you.";

        @Setting(comment = "The message the user gets disconnected with if the user is not whitelisted")
        private String notWhitelistedMessage = "You are not whitelisted to this server.";
    }

}
