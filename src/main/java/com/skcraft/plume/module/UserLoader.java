package com.skcraft.plume.module;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.auth.Context;
import com.skcraft.plume.common.auth.User;
import com.skcraft.plume.common.auth.UserCache;
import com.skcraft.plume.common.config.Config;
import com.skcraft.plume.common.config.InjectConfig;
import com.skcraft.plume.common.extension.InjectService;
import com.skcraft.plume.common.extension.Service;
import com.skcraft.plume.common.extension.module.Module;
import com.skcraft.plume.common.util.Environment;
import com.skcraft.plume.event.network.PlayerAuthenticateEvent;
import com.skcraft.plume.util.Profiles;
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
        Optional<UserCache> optional = userCache.get();
        if (optional.isPresent()) {
            Date now = new Date();
            UserCache userCache = optional.get();
            UserId userId = Profiles.fromProfile(event.getProfile());
            User user = userCache.getUser(userId);

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
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Optional<UserCache> optional = userCache.get();
        if (optional.isPresent()) {
            UserId userId = Profiles.fromPlayer(event.player);
            optional.get().pin(userId); // Should already be loaded
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Optional<UserCache> optional = userCache.get();
        if (optional.isPresent()) {
            UserId userId = Profiles.fromPlayer(event.player);
            optional.get().unpin(userId);
        }
    }

    private static class UsersConfig {
        @Setting(comment = "The message the user gets disconnected with if the user is not in the database")
        private String noUserMessage = "You are not whitelisted. Ask a friend to /invite you.";

        @Setting(comment = "The message the user gets disconnected with if the user is not whitelisted")
        private String notWhitelistedMessage = "You are not whitelisted to this server.";
    }

}
