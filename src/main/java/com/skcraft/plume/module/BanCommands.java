package com.skcraft.plume.module;

import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Text;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.ban.Ban;
import com.skcraft.plume.common.service.ban.BanManager;
import com.skcraft.plume.common.util.Environment;
import com.skcraft.plume.common.util.concurrent.Deferred;
import com.skcraft.plume.common.util.concurrent.Deferreds;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.common.util.service.InjectService;
import com.skcraft.plume.common.util.service.Service;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.Server;
import com.skcraft.plume.util.concurrent.BackgroundExecutor;
import com.skcraft.plume.util.concurrent.TickExecutorService;
import com.skcraft.plume.util.profile.ProfileLookupException;
import com.skcraft.plume.util.profile.ProfileNotFoundException;
import com.skcraft.plume.util.profile.ProfileService;
import com.skcraft.plume.util.profile.Profiles;
import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import ninja.leaping.configurate.objectmapping.Setting;

import java.util.Date;

import static com.skcraft.plume.common.util.SharedLocale.tr;

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
    public void ban(@Sender ICommandSender sender, String name, @Text String reason) {
        BanManager banMan = this.banManager.provide();

        UserId issuer;
        if (sender instanceof EntityPlayer) {
            issuer = Profiles.fromPlayer((EntityPlayer) sender);
        } else {
            issuer = null;
        }

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    UserId userId = profileService.findUserId(name);

                    Ban currentBan = new Ban();
                    currentBan.setUserId(userId);
                    currentBan.setExpireTime(null);
                    currentBan.setHeuristic(false);
                    currentBan.setIssueBy(issuer);
                    currentBan.setIssueTime(new Date());
                    currentBan.setReason(reason);
                    currentBan.setServer(environment.getServerId());

                    banMan.addBan(currentBan);

                    return userId;
                }, executor.getExecutor())
                .done(userId -> {
                    EntityPlayerMP targetPlayer = Server.findPlayer(name);
                    if (targetPlayer != null) {
                        Server.kick(targetPlayer, config.get().kickMessage);
                    }

                    sender.addChatMessage(Messages.info(tr("bans.userBanned", userId.getName())));
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

    @Command(aliases = "pardon", desc = "Pardon a user")
    @Require("plume.bans.pardon")
    public void pardon(@Sender ICommandSender sender, String name, @Text String reason) {
        BanManager banMan = this.banManager.provide();

        UserId issuer;
        if (sender instanceof EntityPlayer) {
            issuer = Profiles.fromPlayer((EntityPlayer) sender);
        } else {
            issuer = null;
        }

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    UserId userId = profileService.findUserId(name);
                    banMan.pardon(userId, issuer, reason);
                    return userId;
                }, executor.getExecutor())
                .done(userId -> {
                    sender.addChatMessage(Messages.info(tr("bans.userUnbanned", userId.getName())));
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

    private static class BansConfig {
        @Setting(comment = "The message the player sees when s/he is banned")
        private String kickMessage = "Your access has been suspended.";
    }
}
