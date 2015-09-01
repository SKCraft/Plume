package com.skcraft.plume.module;

import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.TeleportHelper;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Set;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "teleports", desc = "Convenience methods for teleporting from and to places")
public class Teleports {

    @Command(aliases = "to", desc = "Teleport to the player specified")
    @Require("plume.teleport.to")
    public void to(@Sender EntityPlayerMP player, EntityPlayerMP target) {
        TeleportHelper.teleport(player, target);
        player.addChatMessage(Messages.info(tr("teleport.success")));
    }

    @Command(aliases = "bring", desc = "Brings a player to you")
    @Require("plume.teleport.bring")
    public void bring(@Sender EntityPlayerMP player, Set<EntityPlayerMP> targets) {
        for (EntityPlayerMP target : targets) {
            TeleportHelper.teleport(target, player);
            target.addChatMessage(Messages.info(tr("teleport.teleportedBy", player.getGameProfile().getName())));
        }
        player.addChatMessage(Messages.info(tr("teleport.success")));
    }
}
