package com.skcraft.plume.module.player;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.CommandException;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.util.concurrent.Deferred;
import com.skcraft.plume.common.util.concurrent.Deferreds;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Location3d;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.NBTConstants;
import com.skcraft.plume.util.OptionalPlayer;
import com.skcraft.plume.util.Server;
import com.skcraft.plume.util.TeleportHelper;
import com.skcraft.plume.util.concurrent.BackgroundExecutor;
import com.skcraft.plume.util.concurrent.TickExecutorService;
import com.skcraft.plume.util.playerdata.BulkOfflinePlayerLookup;
import com.skcraft.plume.util.playerdata.OfflinePlayerLookup;
import com.skcraft.plume.util.playerdata.PlayerDataFiles;
import com.skcraft.plume.util.profile.ProfileLookupException;
import com.skcraft.plume.util.profile.ProfileNotFoundException;
import com.skcraft.plume.util.profile.ProfileService;
import lombok.extern.java.Log;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldServer;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "teleports", desc = "Convenience methods for teleporting from and to places")
@Log
public class Teleports {

    @Inject private ProfileService profileService;
    @Inject private BackgroundExecutor backgroundExecutor;
    @Inject private TickExecutorService tickExecutor;

    @Command(aliases = "to", desc = "Teleport to the player specified")
    @Require("plume.teleport.to")
    public void to(@Sender EntityPlayerMP sender, OptionalPlayer target) {
        if (target.getPlayerEntity() != null) {
            TeleportHelper.teleport(sender, target.getPlayerEntity());
            sender.addChatMessage(Messages.info(tr("teleport.success")));
        } else {
            Deferred<?> deferred = Deferreds
                    .when(new OfflinePlayerLookup(profileService, target.getName()), backgroundExecutor.getExecutor())
                    .filter(userId -> {
                        NBTTagCompound tag = PlayerDataFiles.readPlayer(userId);
                        int dimension = tag.getInteger("Dimension");
                        NBTTagList pos = tag.getTagList("Pos", NBTConstants.DOUBLE_TAG);
                        if (pos.tagCount() == 3) {
                            WorldServer world = Server.getDimensionOrLoad(dimension);
                            if (world != null) {
                                TeleportHelper.teleport(sender, new Location3d(world, pos.func_150309_d(0), pos.func_150309_d(1), pos.func_150309_d(2)));
                                sender.addChatMessage(Messages.info(tr("teleport.success")));
                                return null;
                            } else {
                                throw new CommandException(tr("unableToReadPlayerData"));
                            }
                        } else {
                            throw new CommandException(tr("unableToReadPlayerData"));
                        }
                    }, tickExecutor)
                    .fail(e -> {
                        if (e instanceof ProfileNotFoundException) {
                            sender.addChatMessage(Messages.error(tr("args.minecraftUserNotFound", ((ProfileNotFoundException) e).getName())));
                        } else if (e instanceof ProfileLookupException) {
                            sender.addChatMessage(Messages.error(tr("args.minecraftUserLookupFailed", ((ProfileLookupException) e).getName())));
                        } else if (e instanceof CommandException) {
                            sender.addChatMessage(Messages.error(e.getMessage()));
                        } else {
                            sender.addChatMessage(Messages.exception(e));
                        }
                    }, tickExecutor);

            backgroundExecutor.notifyOnDelay(deferred, sender);
        }
    }

    @Command(aliases = "bring", desc = "Brings a player to you")
    @Require("plume.teleport.bring")
    public void bring(@Sender EntityPlayerMP sender, Set<OptionalPlayer> targets) {
        List<String> offline = Lists.newArrayList();
        List<EntityPlayerMP> online = Lists.newArrayList();

        for (OptionalPlayer target : targets) {
            if (target.getPlayerEntity() != null) {
                online.add(target.getPlayerEntity());
            } else {
                offline.add(target.getName());
            }
        }

        if (offline.isEmpty()) {
            for (EntityPlayerMP target : online) {
                TeleportHelper.teleport(target, sender);
                target.addChatMessage(Messages.info(tr("teleport.teleportedBy", sender.getGameProfile().getName())));
            }

            sender.addChatMessage(Messages.info(tr("teleport.success")));
        } else {
            // Time to handle the offline players
            Deferred<?> deferred = Deferreds
                    .when(new BulkOfflinePlayerLookup(profileService, offline), backgroundExecutor.getExecutor())
                    .filter(userIds -> {
                        for (UserId userId : userIds) {
                            EntityPlayerMP player = Server.findPlayer(userId.getUuid());

                            // The player could have logged on while were looking up UUIDs
                            if (player != null) {
                                online.add(player);
                            } else {
                                try {
                                    NBTTagCompound tag = PlayerDataFiles.readPlayer(userId);
                                    tag.setInteger("Dimension", sender.worldObj.provider.dimensionId);
                                    NBTTagList pos = new NBTTagList();
                                    pos.appendTag(new NBTTagDouble(sender.posX));
                                    pos.appendTag(new NBTTagDouble(sender.posY));
                                    pos.appendTag(new NBTTagDouble(sender.posZ));
                                    tag.setTag("Pos", pos);
                                    PlayerDataFiles.writePlayer(userId, tag);
                                    log.info("Teleported the offline player " + userId + " for " + sender);
                                } catch (IOException e) {
                                    sender.addChatMessage(Messages.error(tr("teleport.unableToTeleportOffline", userId.getName())));
                                    log.log(Level.WARNING, "Failed to update the location of " + userId, e);
                                }
                            }
                        }

                        for (EntityPlayerMP target : online) {
                            TeleportHelper.teleport(target, sender);
                            target.addChatMessage(Messages.info(tr("teleport.teleportedBy", sender.getGameProfile().getName())));
                        }

                        return null;
                    }, tickExecutor)
                    .done(value -> {
                        sender.addChatMessage(Messages.info(tr("teleport.success")));
                    })
                    .fail(e -> {
                        if (e instanceof ProfileNotFoundException) {
                            sender.addChatMessage(Messages.error(tr("args.minecraftUserNotFound", ((ProfileNotFoundException) e).getName())));
                        } else if (e instanceof ProfileLookupException) {
                            sender.addChatMessage(Messages.error(tr("args.minecraftUserLookupFailed", ((ProfileLookupException) e).getName())));
                        } else if (e instanceof CommandException) {
                            sender.addChatMessage(Messages.error(e.getMessage()));
                        } else {
                            sender.addChatMessage(Messages.exception(e));
                        }
                    }, tickExecutor);

            backgroundExecutor.notifyOnDelay(deferred, sender);
        }
    }
}
