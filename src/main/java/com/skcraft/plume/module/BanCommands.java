package com.skcraft.plume.module;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Text;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.auth.Group;
import com.skcraft.plume.common.service.auth.User;
import com.skcraft.plume.common.service.ban.Ban;
import com.skcraft.plume.common.service.ban.BanManager;
import com.skcraft.plume.common.util.Environment;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.common.util.service.InjectService;
import com.skcraft.plume.common.util.service.Service;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.ProfileService;
import com.skcraft.plume.util.Profiles;
import com.skcraft.plume.util.concurrent.BackgroundExecutor;
import com.skcraft.plume.util.concurrent.TickExecutorService;
import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import ninja.leaping.configurate.objectmapping.Setting;

import java.io.IOException;
import java.util.Date;


@Module(name = "ban-commands")
@Log
public class BanCommands {

    @Inject private BackgroundExecutor executor;
    @Inject private ProfileService profileService;
    @InjectService private Service<BanManager> banManager;
    @Inject private TickExecutorService tickExecutorService;
    @Inject private Environment environment;
    @InjectConfig("ban_commands") private Config<BansConfig> config;


    @Command(aliases = "ban", desc = "Ban a user")
    @Require("plume.bans.ban")
    public void ban(ICommandSender sender, String name, @Text String reason) {
        BanManager banMan = this.banManager.provide();

        UserId issuer;
        if (sender instanceof EntityPlayer) {
            issuer = Profiles.fromPlayer((EntityPlayer) sender);
        } else {
            issuer = null;
        }

        ListenableFuture<?> future = executor.getExecutor().submit(() -> {
            UserId userId;
            try {
                userId = profileService.findUserId(name);
            } catch (IOException e) {
                sender.addChatMessage(Messages.error("Couldn't look up the user information for '" + name + "'."));
                return;
            }

            if (userId != null) {
                Ban currentBan = new Ban();
                currentBan.setUserId(userId);
                currentBan.setExpireTime(null);
                currentBan.setHeuristic(false);
                currentBan.setIssueBy(issuer);
                currentBan.setIssueTime(new Date());
                currentBan.setReason(reason);
                currentBan.setServer(environment.getServerId());

                banMan.addBan(currentBan);

                tickExecutorService.execute(() -> {
                    EntityPlayerMP targetPlayer = MinecraftServer.getServer().getConfigurationManager().func_152612_a(name);
                    if (targetPlayer != null) targetPlayer.playerNetServerHandler.kickPlayerFromServer(config.get().kickMessage);
                    sender.addChatMessage(Messages.info(currentBan.getUserId().getName() + " has been banned from the server."));
                });
            } else {
                sender.addChatMessage(Messages.error("Couldn't find a Minecraft account with the name '" + name + "'."));
            }
        });

        executor.addCallbacks(future, sender);

    }

    @Command(aliases = "pardon", desc = "Pardon a user")
    @Require("plume.bans.pardon")
    public void pardon(ICommandSender sender, String name, @Text String reason) {
        BanManager banMan = this.banManager.provide();

        UserId issuer;
        if (sender instanceof EntityPlayer) {
            issuer = Profiles.fromPlayer((EntityPlayer) sender);
        } else {
            issuer = null;
        }

        ListenableFuture<?> future = executor.getExecutor().submit(() -> {
            UserId userId;
            try {
                userId = profileService.findUserId(name);
            } catch (IOException e) {
                sender.addChatMessage(Messages.error("Couldn't look up the user information for '" + name + "'."));
                return;
            }

            if (userId != null) {
                banMan.pardon(userId, issuer, reason);

                tickExecutorService.execute(() -> {
                    sender.addChatMessage(Messages.info(userId.getName() + " has been pardoned from the server."));
                });
            } else {
                sender.addChatMessage(Messages.error("Couldn't find a Minecraft account with the name '" + name + "'."));
            }
        });

        executor.addCallbacks(future, sender);
    }

    private static class BansConfig {
        @Setting(comment = "The message the player sees when s/he is banned")
        private String kickMessage = "Your access has been suspended.";
    }
}
