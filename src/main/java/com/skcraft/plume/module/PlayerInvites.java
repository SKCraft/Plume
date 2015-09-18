package com.skcraft.plume.module;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.auth.Group;
import com.skcraft.plume.common.service.auth.User;
import com.skcraft.plume.common.service.auth.UserCache;
import com.skcraft.plume.common.util.concurrent.Deferred;
import com.skcraft.plume.common.util.concurrent.Deferreds;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.concurrent.BackgroundExecutor;
import com.skcraft.plume.util.concurrent.TickExecutorService;
import com.skcraft.plume.util.profile.ProfileLookupException;
import com.skcraft.plume.util.profile.ProfileNotFoundException;
import com.skcraft.plume.util.profile.ProfileService;
import com.skcraft.plume.util.profile.Profiles;
import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "player-invites", desc = "Adds an /invite command so players can whitelist their friends and family")
@Log
public class PlayerInvites {

    @Inject private BackgroundExecutor executor;
    @Inject private ProfileService profileService;
    @Inject private UserCache userCache;
    @Inject private TickExecutorService tickExecutorService;

    @Command(aliases = "invite", desc = "Invite a user")
    @Require("plume.invite")
    public void invite(@Sender ICommandSender sender, String name) {
        UserId referrer;
        if (sender instanceof EntityPlayer) {
            referrer = Profiles.fromPlayer((EntityPlayer) sender);
        } else {
            referrer = null;
        }

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    UserId userId = profileService.findUserId(name);

                    Map<UserId, User> map = userCache.getHive().findUsersById(Lists.newArrayList(userId));
                    User user;

                    if (map.containsKey(userId)) {
                        user = map.get(userId);
                    } else {
                        Set<Group> groups = Sets.newConcurrentHashSet();
                        groups.addAll(userCache.getHive().getLoadedGroups().stream().filter(Group::isAutoJoin).collect(Collectors.toList()));

                        user = new User();
                        user.setUserId(userId);
                        user.setCreateDate(new Date());
                        user.setJoinDate(null);
                        user.setReferrer(referrer);
                        user.setGroups(groups);
                        userCache.getHive().saveUser(user, true);
                    }

                    return userId;
                }, executor.getExecutor())
                .done(userId -> {
                    sender.addChatMessage(Messages.info(tr("invites.userInvited", userId.getName())));
                }, tickExecutorService)
                .fail(e -> {
                    if (e instanceof ProfileNotFoundException) {
                        sender.addChatMessage(Messages.error(tr("args.minecraftUserNotFound", ((ProfileNotFoundException) e).getName())));
                    } else if (e instanceof ProfileLookupException) {
                        sender.addChatMessage(Messages.error(tr("args.minecraftUserLookupFailed", ((ProfileLookupException) e).getName())));
                    } else {
                        sender.addChatMessage(Messages.exception(e));
                    }
                }, tickExecutorService);

        executor.notifyOnDelay(deferred, sender);
    }

}
