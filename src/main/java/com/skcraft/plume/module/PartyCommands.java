package com.skcraft.plume.module;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.CommandException;
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
import net.minecraft.util.ChatComponentText;

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

                    return party;
                }, executor.getExecutor())
                .done(party -> {
                    sender.addChatMessage(Messages.info(tr("party.create.success", party.getName())));
                }, tickExecutorService)
                .fail(e -> {
                    if (e instanceof PartyExistsException) {
                        sender.addChatMessage(Messages.error(tr("party.create.exists", name)));
                    } else {
                        sender.addChatMessage(Messages.error(tr("args.exception.unhandled", e.getMessage())));
                    }
                }, tickExecutorService);

        executor.notifyOnDelay(deferred, sender);
    }

    @Command(aliases = "add", desc = "Adds a player to a party")
    @Group(@At("party"))
    //@Require("plume.party.add") //TODO uncomment this when online
    public void add(@Sender EntityPlayer sender, String name, @Text String invitee) {
        PartyCache partyMan = this.partyCache.provide();
        UserId issuer = Profiles.fromPlayer(sender);

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    UserId userId = profileService.findUserId(invitee);
                    Party party = partyMan.getParty(name);

                    if (party == null) {
                        throw new CommandException(tr("party.exception.nonexistant"));
                    } else if (!Parties.canManage(party, issuer)) {
                        throw new CommandException(tr("party.exception.cannotmanage"));
                    } else {
                        partyMan.addMembers(party, Sets.newHashSet(new Member(userId, Rank.MEMBER)));
                        partyMan.getManager().refreshParty(party);

                        return userId.getName();
                    }
                },executor.getExecutor())
                .done(userName -> {
                    sender.addChatMessage(Messages.info(tr("party.invite.success", userName)));
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

     @Command(aliases = "remove", desc = "Removes a player from a party")
     @Group(@At("party"))
     //@Require("plume.party.remove") //TODO uncomment this when online
     public void remove(@Sender EntityPlayer sender, String name, @Text String removee) {
        PartyCache partyMan = this.partyCache.provide();
        UserId issuer = Profiles.fromPlayer(sender);

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    UserId userId = profileService.findUserId(removee);
                    Party party = partyMan.getParty(name);

                    if (party == null) {
                        throw new CommandException(tr("party.exception.nonexistant"));
                    } else if (!Parties.canManage(party, issuer)) {
                        throw new CommandException(tr("party.exception.cannotmanage"));
                    } else {
                        partyMan.removeMembers(party, Sets.newHashSet(Parties.getMemberByUser(party, userId)));
                        partyMan.getManager().refreshParty(party);

                        return userId.getName();
                    }
                },executor.getExecutor())
                .done(userName -> {
                    sender.addChatMessage(Messages.info(tr("party.remove.success", userName)));
                }, tickExecutorService)
                .fail(e -> {
                    if (e instanceof ProfileNotFoundException) {
                        sender.addChatMessage(Messages.error(tr("args.minecraftUserNotFound", ((ProfileNotFoundException) e).getName())));
                    } else if (e instanceof ProfileLookupException) {
                        sender.addChatMessage(Messages.error(tr("args.minecraftUserLookupFailed", ((ProfileLookupException) e).getName())));
                    } else if (e instanceof CommandException) {
                        sender.addChatMessage(Messages.error(tr("party.remove.failed", e.getMessage())));
                    } else {
                        sender.addChatMessage(Messages.error(tr("args.exception.unhandled", ((ProfileLookupException) e).getName())));
                    }
                }, tickExecutorService);

        executor.notifyOnDelay(deferred, sender);
    }

    @Command(aliases = "info", desc = "Displays info about a party")
    @Group(@At("party"))
    //@Require("plume.party.info") //TODO uncomment this when online
    public void info(@Sender EntityPlayer sender, String name) {
        PartyCache partyMan = this.partyCache.provide();
        UserId issuer = Profiles.fromPlayer(sender);

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    Party party = partyMan.getParty(name);

                    if (party == null) {
                        throw new CommandException(tr("party.exception.nonexistant"));
                    } else if (!party.getMembers().contains(new Member(issuer, Rank.MEMBER))) { // rank doesn't matter here since contains() can't check rank
                        throw new CommandException(tr("party.exception.nonmember"));
                    } else {
                        return party;
                    }
                },executor.getExecutor())
                .done(party -> {
                    sender.addChatMessage(new ChatComponentText(tr("party.info.1", party.getName())));
                    sender.addChatMessage(new ChatComponentText(tr("party.info.2", party.getCreateTime().toString())));
                    sender.addChatMessage(new ChatComponentText(Parties.getMemberListStr(party)));
                }, tickExecutorService)
                .fail(e -> {
                    if (e instanceof CommandException) {
                        sender.addChatMessage(Messages.error(tr("party.info.exception", e.getMessage())));
                    } else {
                        sender.addChatMessage(Messages.error(tr("args.exception.unhandled", ((ProfileLookupException) e).getName())));
                    }
                }, tickExecutorService);

        executor.notifyOnDelay(deferred, sender);
    }



    //TODO make this entire thing less hacky once sk adds a queries for it
    @Command(aliases = "delete", desc = "Deletes a new party")
    @Group(@At("partymanage"))
    //@Require("plume.partymanage.delete") //TODO uncomment this when online
    public void delete(@Sender EntityPlayer sender, String name) {
        PartyCache partyMan = this.partyCache.provide();
        UserId issuer = Profiles.fromPlayer(sender);

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    Party party = partyMan.getManager().findPartyByName(name);

                    if (party == null) {
                        throw new CommandException(tr("party.exception.nonexistant"));
                    } else if (!Parties.canManage(party, issuer)) {
                        throw new CommandException(tr("party.exception.cannotmanage"));
                    } else {
                        partyMan.removeMembers(party, party.getMembers()); //TODO make this less hacky, i.e. add a sql query to delete a party
                        partyMan.getManager().refreshParty(party);
                        return true;
                    }

                }, executor.getExecutor())
                .done(condition -> {
                    sender.addChatMessage(Messages.info(tr("party.delete.success", name)));
                }, tickExecutorService)
                .fail(e -> {
                    if (e instanceof CommandException) {
                        sender.addChatMessage(Messages.error(tr("party.delete.failed.other", e.getMessage())));
                    } else {
                        sender.addChatMessage(Messages.error(tr("args.exception.unhandled", e.getLocalizedMessage())));
                    }
                }, tickExecutorService);

        executor.notifyOnDelay(deferred, sender);
    }
}
