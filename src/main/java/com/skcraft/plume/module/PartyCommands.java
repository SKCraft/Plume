package com.skcraft.plume.module;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.CommandException;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Text;
import com.skcraft.plume.command.At;
import com.skcraft.plume.command.Group;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.party.*;
import com.skcraft.plume.common.util.concurrent.Deferred;
import com.skcraft.plume.common.util.concurrent.Deferreds;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.common.util.service.InjectService;
import com.skcraft.plume.common.util.service.Service;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.profile.ProfileLookupException;
import com.skcraft.plume.util.profile.ProfileNotFoundException;
import com.skcraft.plume.util.profile.ProfileService;
import com.skcraft.plume.util.profile.Profiles;
import com.skcraft.plume.util.concurrent.BackgroundExecutor;
import com.skcraft.plume.util.concurrent.TickExecutorService;
import lombok.extern.java.Log;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import org.javatuples.Triplet;
import java.util.Date;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "party-commands")
@Log
public class PartyCommands {
    @Inject private BackgroundExecutor executor;
    @Inject private ProfileService profileService;
    @Inject private TickExecutorService tickExecutorService;
    @InjectService private Service<PartyCache> partyCache;

    @Command(aliases = "create", desc = "Create a new party")
    @Group(@At("party"))
    //@Require("plume.party.create") //TODO uncomment this when online
    public void create(@Sender EntityPlayer sender, String name) {
        PartyCache partyMan = this.partyCache.provide();
        UserId issuer = Profiles.fromPlayer(sender);

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    Party party = new Party();
                    party.setName(name);
                    party.setCreateTime(new Date());
                    party.setMembers(Sets.newHashSet(new Member(issuer, Rank.OWNER)));
                    partyMan.addParty(party);
                    partyMan.getManager().refreshParty(party);

                    return null;
                }, executor.getExecutor())
                .done(probNull -> { //TODO find a way to not pass anything (instead of probNull)
                    sender.addChatMessage(Messages.info(tr("party.create.success", name)));
                }, tickExecutorService)
                .fail(e -> {
                    if (e instanceof PartyExistsException) {
                        sender.addChatMessage(Messages.error(tr("party.create.exists", name)));
                    }
                }, tickExecutorService);

        executor.notifyOnDelay(deferred, sender);
    }

    @Command(aliases = "delete", desc = "Delete a new party")
    @Group(@At("party"))
    //@Require("plume.party.delete") //TODO uncomment this when online
    public void delete(@Sender EntityPlayer sender, String name) {
        PartyCache partyMan = this.partyCache.provide();
        UserId issuer = Profiles.fromPlayer(sender);

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    Party party = partyMan.getManager().findPartyByName(name);
                    if (party != null && party.getMembers().contains(new Member(issuer, Rank.OWNER))) {
                        partyMan.removeMembers(party, party.getMembers()); //TODO make this less hacky, i.e. add a sql query to delete a party
                        partyMan.getManager().refreshParty(party);
                        return true;
                    } else {
                        return false;
                    }
                }, executor.getExecutor())
                .done(condition -> {
                    if(condition) sender.addChatMessage(Messages.info(tr("party.delete.success", name)));
                    else sender.addChatMessage(Messages.error(tr("party.delete.failed.other", name)));
                }, tickExecutorService)
                .fail(e -> {
                    sender.addChatMessage(Messages.error(tr("party.delete.failed.exception", e)));
                }, tickExecutorService);

        executor.notifyOnDelay(deferred, sender);
    }

    @Command(aliases = "invite", desc = "Invites a player to a party")
    @Group(@At("party"))
    //@Require("plume.party.invite") //TODO uncomment this when online
    public void invite(@Sender EntityPlayer sender, String name, @Text String invitee) {
        PartyCache partyMan = this.partyCache.provide();
        UserId issuer = Profiles.fromPlayer(sender);

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    UserId userId = profileService.findUserId(invitee);
                    Party party = partyMan.getParty(name);

                    if (party == null) {
                        throw new CommandException("Party does not exist");
                    } else if (!Parties.canManage(party, userId)) {
                        throw new CommandException("You are not the owner or manager of this party");
                    } else {
                        partyMan.addMembers(party, Sets.newHashSet(new Member(userId, Rank.MEMBER)));
                        partyMan.getManager().refreshParty(party);

                        return new Triplet<>(issuer.getName(), party.getName(), userId.getName());
                    }
                },executor.getExecutor())
                .done(rl -> {
                    sender.addChatMessage(Messages.info(tr("party.invite.success.issuer", rl.getValue2())));
                    EntityPlayerMP targetPlayer = MinecraftServer.getServer().getConfigurationManager().func_152612_a(rl.getValue2());
                    targetPlayer.addChatMessage(Messages.info(tr("party.invite.success.target", rl.getValue1(), rl.getValue0())));
                }, tickExecutorService)
                .fail(e -> {
                    if (e instanceof ProfileNotFoundException) {
                        sender.addChatMessage(Messages.error(tr("args.minecraftUserNotFound", ((ProfileNotFoundException) e).getName())));
                    } else if (e instanceof ProfileLookupException) {
                        sender.addChatMessage(Messages.error(tr("args.minecraftUserLookupFailed", ((ProfileLookupException) e).getName())));
                    } else if (e instanceof CommandException) {
                        sender.addChatMessage(Messages.error(tr("party.invite.failed", e.getMessage())));
                    } else {
                        sender.addChatMessage(Messages.error(tr("args.exception.unhandled", ((ProfileLookupException) e).getName())));
                    }
                }, tickExecutorService);

        executor.notifyOnDelay(deferred, sender);
    }

        @Command(aliases = "remove", desc = "Removes a player to a party")
    @Group(@At("party"))
    //@Require("plume.party.remove") //TODO uncomment this when online
    public void remove(@Sender EntityPlayer sender, String name, @Text String removee) {
        PartyCache partyMan = this.partyCache.provide();
        UserId issuer = Profiles.fromPlayer(sender);

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    UserId userId = profileService.findUserId(removee);

                    Party party = partyMan.getParty(name);
                    partyMan.addMembers(party, Sets.newHashSet(new Member(userId, Rank.MEMBER)));
                    partyMan.getManager().refreshParty(party);

                    //return userId;
                    return Lists.newArrayList(userId.getName(), party.getName(), issuer.getName());
                }, executor.getExecutor())
                .done(rl -> {
                    sender.addChatMessage(Messages.info(tr("party.invite.success.issuer", rl.get(0))));
                    EntityPlayerMP targetPlayer = MinecraftServer.getServer().getConfigurationManager().func_152612_a(rl.get(0));
                    targetPlayer.addChatMessage(Messages.info(tr("party.invite.success.target", rl.get(1), rl.get(2))));
                }, tickExecutorService)
                .fail(e -> {
                    if (e instanceof ProfileNotFoundException) {
                        sender.addChatMessage(Messages.error(tr("args.minecraftUserNotFound", ((ProfileNotFoundException) e).getName())));
                    } else if (e instanceof ProfileLookupException) {
                        sender.addChatMessage(Messages.error(tr("args.minecraftUserLookupFailed", ((ProfileLookupException) e).getName())));
                    } else {
                        sender.addChatMessage(Messages.error(tr("args.exception.unhandled", ((ProfileLookupException) e).getName())));
                    }
                }, tickExecutorService);

        executor.notifyOnDelay(deferred, sender);
    }
}
