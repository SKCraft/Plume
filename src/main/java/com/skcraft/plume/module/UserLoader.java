package com.skcraft.plume.module;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.auth.Context;
import com.skcraft.plume.common.service.auth.User;
import com.skcraft.plume.common.service.auth.UserCache;
import com.skcraft.plume.common.util.Environment;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.event.network.PlayerAuthenticateEvent;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.Server;
import com.skcraft.plume.util.profile.Profiles;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import ninja.leaping.configurate.objectmapping.Setting;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "user-loader", hidden = true)
public class UserLoader {

    private final Cache<User, Boolean> expireCache = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build();
    private final Map<UserId, User> online = Maps.newHashMap();

    @InjectConfig("users") private Config<UsersConfig> config;
    @Inject private UserCache userCache;
    @Inject private Environment environment;

    @SubscribeEvent
    public void onAuthenticate(PlayerAuthenticateEvent event) {
        if (userCache != null) {
            Date now = new Date();
            UserId userId = Profiles.fromProfile(event.getProfile());
            User user = userCache.load(userId);

            if (user != null) {
                // Keep a strong reference so the user cache keeps the object long enough
                // for us to reach PlayerLoggedInEvent
                expireCache.put(user, true);

                if (user.getSubject().hasPermission("whitelist", environment.update(new Context.Builder()).build())) {
                    user.getUserId().setName(event.getProfile().getName());
                    if (user.getJoinDate() == null && user.getCreateDate() != null) {
                        user.setJoinDate(now);
                    }
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
        if (userCache != null) {
            UserId userId = Profiles.fromPlayer(event.player);
            User user = userCache.getIfPresent(userId);
            if (user != null) {
                online.put(userId, user);

                Date joinDate = user.getJoinDate();
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MINUTE, -1);
                Date after = calendar.getTime();

                if (config.get().announceNewPlayers && joinDate != null && joinDate.after(after)) {
                    ChatComponentText message = new ChatComponentText(tr("users.newMember", event.player.getGameProfile().getName()));
                    message.getChatStyle().setColor(EnumChatFormatting.GOLD);
                    Messages.broadcast(message, player -> !player.equals(event.player));
                }
            } else if (!config.get().allowLoginWithoutLoadedUser) {
                Server.kick((EntityPlayerMP) event.player, tr("users.profileNotLoaded"));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UserId userId = Profiles.fromPlayer(event.player);
        online.remove(userId);
    }

    private static class UsersConfig {
        @Setting(comment = "Set to true to permit players who don't have his or her user information loaded to login anyway")
        private boolean allowLoginWithoutLoadedUser = false;

        @Setting(comment = "The message the user gets disconnected with if the user is not in the database")
        private String noUserMessage = "You are not whitelisted. Ask a friend to /invite you.";

        @Setting(comment = "The message the user gets disconnected with if the user is not whitelisted")
        private String notWhitelistedMessage = "You are not whitelisted to this server.";

        @Setting(comment = "Whether to announce that a newly joined player is new to the server")
        private boolean announceNewPlayers = false;
    }

}
