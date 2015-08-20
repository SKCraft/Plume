package com.skcraft.plume.module;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.auth.Group;
import com.skcraft.plume.common.auth.User;
import com.skcraft.plume.common.auth.UserCache;
import com.skcraft.plume.common.extension.InjectService;
import com.skcraft.plume.common.extension.Service;
import com.skcraft.plume.common.extension.module.Module;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.ProfileService;
import com.skcraft.plume.util.Profiles;
import com.skcraft.plume.util.concurrent.BackgroundExecutor;
import com.skcraft.plume.util.concurrent.TickExecutorService;
import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Module(name = "player-invites")
@Log
public class PlayerInvites {

    @Inject private BackgroundExecutor executor;
    @Inject private ProfileService profileService;
    @InjectService private Service<UserCache> userCache;
    @Inject private TickExecutorService tickExecutorService;

    @Command(aliases = "invite", desc = "Invite a user")
    @Require("plume.invite")
    public void invite(ICommandSender sender, String name) {
        Optional<UserCache> optional = this.userCache.get();
        if (optional.isPresent()) {
            UserCache userCache = optional.get();

            UserId referrer;
            if (sender instanceof EntityPlayer) {
                referrer = Profiles.fromPlayer((EntityPlayer) sender);
            } else {
                referrer = null;
            }

            ListenableFuture<?> future = executor.getExecutor().submit(() -> {
                UserId userId = null;
                try {
                    userId = profileService.findUserId(name);
                } catch (IOException e) {
                    sender.addChatMessage(Messages.error("Couldn't look up the user information for '" + name + "'."));
                }

                if (userId != null) {
                    Map<UserId, User> map = userCache.getHive().findUsersById(Lists.newArrayList(userId));
                    User user;

                    if (map.containsKey(userId)) {
                        user = map.get(userId);
                    } else {
                        Set<Group> groups = Sets.newConcurrentHashSet();
                        groups.addAll(userCache.getHive().getLoadedGroups().stream().filter(Group::isAutoJoin).collect(Collectors.toList()));

                        user = new User();
                        user.setUserId(userId);
                        user.setJoinDate(new Date());
                        user.setReferrer(referrer);
                        user.setGroups(groups);
                        userCache.getHive().saveUser(user, true);
                    }

                    tickExecutorService.execute(() -> {
                        sender.addChatMessage(Messages.info(user.getUserId().getName() + " has been invited to the server."));
                    });
                } else {
                    sender.addChatMessage(Messages.error("Couldn't find a Minecraft account with the name '" + name + "'."));
                }
            });

            executor.addCallbacks(future, sender);
        } else {
            sender.addChatMessage(Messages.error("The invite cannot be used at this time."));
        }
    }

}
