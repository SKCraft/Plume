package com.skcraft.plume.module.player;

import com.google.common.collect.Sets;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Optional;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Location3d;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.TeleportHelper;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;

import java.util.Set;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "spawns", desc = "Commands to set a world's spawn and to teleport to it")
public class Spawns {

    @Command(aliases = "setspawn", desc = "Set the spawn of the current world")
    @Require("plume.spawns.setspawn")
    public void setSpawn(@Sender EntityPlayer player) {
        player.worldObj.setSpawnLocation((int) player.posX, (int) player.posY, (int) player.posZ);
        player.addChatMessage(Messages.info(tr("spawns.setSpawn")));
    }

    @Command(aliases = "spawn", desc = "Teleport to the current world's spawn")
    @Require("plume.spawns.spawn")
    public void spawn(@Sender ICommandSender sender, @Optional Set<EntityPlayerMP> targets) {
        if (!(sender instanceof EntityPlayerMP) && targets == null) {
            sender.addChatMessage(Messages.error(tr("args.playerMustBeSpecified")));
        } else {
            if (targets == null) {
                targets = Sets.newHashSet((EntityPlayerMP) sender);
            }

            for (EntityPlayerMP player : targets) {
                ChunkCoordinates spawnPoint = player.worldObj.getSpawnPoint();
                TeleportHelper.teleport(player, new Location3d(player.worldObj, spawnPoint.posX + 0.5, spawnPoint.posY, spawnPoint.posZ + 0.5));
                player.worldObj.setSpawnLocation((int) player.posX, (int) player.posY, (int) player.posZ);
                player.addChatMessage(Messages.info(tr("spawns.teleportedToSpawn")));
            }

            if (targets.size() != 1 || !targets.iterator().next().equals(sender)) {
                sender.addChatMessage(Messages.info(tr("spawns.teleportedPlayersToLocalSpawn")));
            }
        }
    }

}
