package com.skcraft.plume.module;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.CommandException;
import com.sk89q.intake.Require;
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

import java.util.Date;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "parties", desc = "Provides commands to manage parties [requires party service]")
@Log
public class Parties {

    @Inject private BackgroundExecutor executor;
    @Inject private ProfileService profileService;
    @Inject private TickExecutorService tickExecutorService;
    @InjectService private Service<PartyCache> partyCache;

    @Command(aliases = "create", usage = "/party create [name]", desc = "Create a new party")
    @Group(@At("party"))
    @Require("plume.party.create")
    public void create(@Sender EntityPlayer sender, String name) {
        PartyCache partyMan = this.partyCache.provide();
        UserId issuer = Profiles.fromPlayer(sender);

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    Party party = new Party();
                    party.setName(name);
                    party.setCreateTime(new Date());
                    party.setMembers(Sets.newHashSet(new Member(issuer, Rank.OWNER)));
                    partyMan.add(party);
                    partyMan.getManager().refreshParty(party);

                    return party;
                }, executor.getExecutor())
                .done(party -> {
                    sender.addChatMessage(Messages.info(tr("party.create.success", party.getName())));
                }, tickExecutorService)
                .fail(e -> {
                    if (e instanceof PartyExistsException) {
                        sender.addChatMessage(Messages.error(tr("party.create.existsAlready", name)));
                    } else {
                        sender.addChatMessage(Messages.error(tr("args.exception.unhandled", e.getMessage())));
                    }
                }, tickExecutorService);

        executor.notifyOnDelay(deferred, sender);
    }

    @Command(aliases = "add", usage = "/party add [party] [username]", desc = "Adds a player to a party")
    @Group(@At("party"))
    @Require("plume.party.add")
    public void add(@Sender EntityPlayer sender, String name, String invitee) {
        PartyCache partyMan = this.partyCache.provide();
        UserId issuer = Profiles.fromPlayer(sender);

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    UserId userId = profileService.findUserId(invitee);
                    Party party = partyMan.get(name);

                    if (party == null) {
                        throw new CommandException(tr("party.doesNotExist"));
                    } else if (!com.skcraft.plume.common.service.party.Parties.canManage(party, issuer)) {
                        throw new CommandException(tr("party.notManager"));
                    } else if (userId.equals(issuer)) {
                        throw new CommandException(tr("party.add.cannotAddSelf"));
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

    @Command(aliases = "remove", usage = "/party remove [party] [username]", desc = "Removes a player from a party")
    @Group(@At("party"))
    @Require("plume.party.remove")
    public void remove(@Sender EntityPlayer sender, String name, String removee) {
        PartyCache partyMan = this.partyCache.provide();
        UserId issuer = Profiles.fromPlayer(sender);

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    UserId userId = profileService.findUserId(removee);
                    Party party = partyMan.get(name);

                    if (party == null) {
                        throw new CommandException(tr("party.doesNotExist"));
                    } else if (!com.skcraft.plume.common.service.party.Parties.canManage(party, issuer)) {
                        throw new CommandException(tr("party.notManager"));
                    } else if (userId.equals(issuer)) {
                        throw new CommandException(tr("party.remove.cannotAddSelf"));
                    } else {
                        partyMan.removeMembers(party, Sets.newHashSet(com.skcraft.plume.common.service.party.Parties.getMemberByUser(party, userId)));
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

    @Command(aliases = "rank", usage = "/party rank [party] [username] [manager|member]", desc = "Changes the rank of a party member")
    @Group(@At("party"))
    @Require("plume.party.rank")
    public void rank(@Sender EntityPlayer sender, String name, String target, String rank) {
        PartyCache partyMan = this.partyCache.provide();
        UserId issuer = Profiles.fromPlayer(sender);

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    UserId userId = profileService.findUserId(target);
                    Party party = partyMan.get(name);

                    if (party == null) {
                        throw new CommandException(tr("party.doesNotExist"));
                    } else if (!com.skcraft.plume.common.service.party.Parties.canManage(party, issuer)) {
                        throw new CommandException(tr("party.notManager"));
                    } else if (userId.equals(issuer)) {
                        throw new CommandException(tr("party.rank.cannotChangeSelf"));
                    } else {
                        switch(rank.toLowerCase()) {
                            case "member":
                                partyMan.addMembers(party, Sets.newHashSet(new Member(userId, Rank.MEMBER)));
                                partyMan.getManager().refreshParty(party);
                                break;
                            case "manager":
                                partyMan.addMembers(party, Sets.newHashSet(new Member(userId, Rank.MANAGER)));
                                partyMan.getManager().refreshParty(party);
                                break;
                            default:
                                throw new CommandException("Invalid rank argument - Use member or manager.");
                        }

                        return userId.getName();
                    }
                },executor.getExecutor())
                .done(username -> {
                    sender.addChatMessage(Messages.info(tr("party.rank.success", username)));
                }, tickExecutorService)
                .fail(e -> {
                    if (e instanceof ProfileNotFoundException) {
                        sender.addChatMessage(Messages.error(tr("args.minecraftUserNotFound", ((ProfileNotFoundException) e).getName())));
                    } else if (e instanceof ProfileLookupException) {
                        sender.addChatMessage(Messages.error(tr("args.minecraftUserLookupFailed", ((ProfileLookupException) e).getName())));
                    } else if (e instanceof CommandException) {
                        sender.addChatMessage(Messages.error(tr("party.rank.failed", e.getMessage())));
                    } else {
                        sender.addChatMessage(Messages.error(tr("args.exception.unhandled", ((ProfileLookupException) e).getName())));
                    }
                }, tickExecutorService);

        executor.notifyOnDelay(deferred, sender);
    }

    @Command(aliases = "leave", usage = "/party leave [party]", desc = "Leave a party you're a member/manager of")
    @Group(@At("party"))
    @Require("plume.party.leave")
    public void leave(@Sender EntityPlayer sender, String name) {
        PartyCache partyMan = this.partyCache.provide();
        UserId issuer = Profiles.fromPlayer(sender);

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    Party party = partyMan.get(name);

                    if (party == null) {
                        throw new CommandException(tr("party.doesNotExist"));
                    } else {
                        Member member = com.skcraft.plume.common.service.party.Parties.getMemberByUser(party, issuer);
                        if (member == null) {
                            throw new CommandException(tr("party.notMember"));
                        } else if (member.getRank().equals(Rank.OWNER)) {
                            throw new CommandException(tr("party.cannotRemoveSelf"));
                        } else {
                            partyMan.removeMembers(party, Sets.newHashSet(com.skcraft.plume.common.service.party.Parties.getMemberByUser(party, issuer)));
                            partyMan.getManager().refreshParty(party);

                            return null;
                        }
                    }
                },executor.getExecutor())
                .done(bogusfieldname -> {
                    sender.addChatMessage(Messages.info(tr("party.leave.success")));
                }, tickExecutorService)
                .fail(e -> {
                    if (e instanceof ProfileNotFoundException) {
                        sender.addChatMessage(Messages.error(tr("args.minecraftUserNotFound", ((ProfileNotFoundException) e).getName())));
                    } else if (e instanceof ProfileLookupException) {
                        sender.addChatMessage(Messages.error(tr("args.minecraftUserLookupFailed", ((ProfileLookupException) e).getName())));
                    } else if (e instanceof CommandException) {
                        sender.addChatMessage(Messages.error(tr("party.leave.failed", e.getMessage())));
                    } else {
                        sender.addChatMessage(Messages.error(tr("args.exception.unhandled", ((ProfileLookupException) e).getName())));
                    }
                }, tickExecutorService);

        executor.notifyOnDelay(deferred, sender);
    }

    @Command(aliases = "info", usage = "/party info [party]", desc = "Displays info about a party")
    @Group(@At("party"))
    @Require("plume.party.info")
    public void info(@Sender EntityPlayer sender, String name) {
        PartyCache partyMan = this.partyCache.provide();
        UserId issuer = Profiles.fromPlayer(sender);

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    Party party = partyMan.get(name);

                    if (party == null) {
                        throw new CommandException(tr("party.doesNotExist"));
                    } else if (!party.getMembers().contains(new Member(issuer, Rank.MEMBER))) { // rank doesn't matter here since contains() can't check rank
                        throw new CommandException(tr("party.notMember"));
                    } else {
                        return party;
                    }
                },executor.getExecutor())
                .done(party -> {
                    sender.addChatMessage(Messages.info(tr("party.info.1", party.getName())));
                    sender.addChatMessage(Messages.info(tr("party.info.2", party.getCreateTime().toString())));
                    sender.addChatMessage(Messages.info(tr("party.info.3")));
                    sender.addChatMessage(Messages.info(com.skcraft.plume.common.service.party.Parties.getMemberListStr(party)));
                }, tickExecutorService)
                .fail(e -> {
                    if (e instanceof CommandException) {
                        sender.addChatMessage(Messages.error(tr("party.info.failed", e.getMessage())));
                    } else {
                        sender.addChatMessage(Messages.error(tr("args.exception.unhandled", ((ProfileLookupException) e).getName())));
                    }
                }, tickExecutorService);

        executor.notifyOnDelay(deferred, sender);
    }

    @Command(aliases = "add", usage = "/partymanage add [party] [username]", desc = "Adds a player to a party")
    @Group(@At("partymanage"))
    @Require("plume.partymanage.add")
    public void manageadd(@Sender EntityPlayer sender, String name, String invitee) {
        PartyCache partyMan = this.partyCache.provide();

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    UserId userId = profileService.findUserId(invitee);
                    Party party = partyMan.get(name);

                    if (party == null) {
                        throw new CommandException(tr("party.doesNotExist"));
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

    @Command(aliases = "remove", usage = "/partymanage remove [party] [username]", desc = "Removes a player from a party")
    @Group(@At("partymanage"))
    @Require("plume.partymanage.remove")
    public void manageremove(@Sender EntityPlayer sender, String name, String removee) {
        PartyCache partyMan = this.partyCache.provide();

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    UserId userId = profileService.findUserId(removee);
                    Party party = partyMan.get(name);

                    if (party == null) {
                        throw new CommandException(tr("party.doesNotExist"));
                    } else if (userId.equals(Profiles.fromPlayer(sender))) {
                        throw new CommandException(tr("party.remove.cannotAddSelf"));
                    } else {
                        partyMan.removeMembers(party, Sets.newHashSet(com.skcraft.plume.common.service.party.Parties.getMemberByUser(party, userId)));
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

    @Command(aliases = "rank", usage = "/partymanage rank [party] [username] [manager|member]", desc = "Changes the rank of a party member")
    @Group(@At("partymanage"))
    @Require("plume.partymanage.rank")
    public void managerank(@Sender EntityPlayer sender, String name, String target, String rank) {
        PartyCache partyMan = this.partyCache.provide();
        UserId issuer = Profiles.fromPlayer(sender);

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    UserId userId = profileService.findUserId(target);
                    Party party = partyMan.get(name);

                    if (party == null) {
                        throw new CommandException(tr("party.doesNotExist"));
                    } else {
                        switch(rank.toLowerCase()) {
                            case "member":
                                partyMan.addMembers(party, Sets.newHashSet(new Member(userId, Rank.MEMBER)));
                                partyMan.getManager().refreshParty(party);
                                break;
                            case "manager":
                                partyMan.addMembers(party, Sets.newHashSet(new Member(userId, Rank.MANAGER)));
                                partyMan.getManager().refreshParty(party);
                                break;
                            default:
                                throw new CommandException("Invalid rank argument - Use member or manager.");
                        }

                        return userId.getName();
                    }
                },executor.getExecutor())
                .done(username -> {
                    sender.addChatMessage(Messages.info(tr("party.rank.success", username)));
                }, tickExecutorService)
                .fail(e -> {
                    if (e instanceof ProfileNotFoundException) {
                        sender.addChatMessage(Messages.error(tr("args.minecraftUserNotFound", ((ProfileNotFoundException) e).getName())));
                    } else if (e instanceof ProfileLookupException) {
                        sender.addChatMessage(Messages.error(tr("args.minecraftUserLookupFailed", ((ProfileLookupException) e).getName())));
                    } else if (e instanceof CommandException) {
                        sender.addChatMessage(Messages.error(tr("party.rank.failed", e.getMessage())));
                    } else {
                        sender.addChatMessage(Messages.error(tr("args.exception.unhandled", ((ProfileLookupException) e).getName())));
                    }
                }, tickExecutorService);

        executor.notifyOnDelay(deferred, sender);
    }

    //TODO make this entire thing less hacky once sk adds a queries for it
    @Command(aliases = "delete", usage = "/party delete [party]", desc = "Deletes a new party")
    @Group(@At("partymanage"))
    @Require("plume.partymanage.delete")
    public void managedelete(@Sender EntityPlayer sender, String name) {
        PartyCache partyMan = this.partyCache.provide();
        UserId issuer = Profiles.fromPlayer(sender);

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    Party party = partyMan.getManager().findPartyByName(name);

                    if (party == null) {
                        throw new CommandException(tr("party.doesNotExist"));
                    } else if (!com.skcraft.plume.common.service.party.Parties.canManage(party, issuer)) {
                        throw new CommandException(tr("party.notManager"));
                    } else {
                        partyMan.removeMembers(party, party.getMembers());
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
