package com.skcraft.plume.module;

import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Optional;
import com.skcraft.plume.command.At;
import com.skcraft.plume.command.Group;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "speed", desc = "Commands to change player walk and flight speeds")
public class Speed {

    private static final float DEFAULT_WALK_SPEED = 0.1f;
    private static final float DEFAULT_FLY_SPEED = 0.05f;

    @Command(aliases = "walk", desc = "Set a player's walk speed")
    @Group(@At("speed"))
    @Require("plume.speed.walk")
    public void walkSpeed(@Sender ICommandSender sender, float speed, @Optional EntityPlayerMP target) {
        if (!(sender instanceof EntityPlayerMP) && target == null) {
            sender.addChatMessage(Messages.error(tr("playerRequired")));
            return;
        }

        EntityPlayerMP player = target != null ? target : (EntityPlayerMP) sender;
        player.capabilities.walkSpeed = speed * DEFAULT_WALK_SPEED;
        player.sendPlayerAbilities();
        sender.addChatMessage(Messages.info(tr("speed.walkSpeedSet", speed)));
        if (!sender.equals(player)) {
            player.addChatMessage(Messages.info(tr("speed.walkSpeedSet", speed)));
        }
    }

    @Command(aliases = {"flight", "fly"}, desc = "Set a player's flight speed")
    @Group(@At("speed"))
    @Require("plume.speed.flight")
    public void flightSpeed(@Sender ICommandSender sender, float speed, @Optional EntityPlayerMP target) {
        if (!(sender instanceof EntityPlayerMP) && target == null) {
            sender.addChatMessage(Messages.error(tr("playerRequired")));
            return;
        }

        EntityPlayerMP player = target != null ? target : (EntityPlayerMP) sender;
        player.capabilities.flySpeed = speed * DEFAULT_FLY_SPEED;
        player.sendPlayerAbilities();
        sender.addChatMessage(Messages.info(tr("speed.flightSpeedSet", speed)));
        if (!sender.equals(player)) {
            player.addChatMessage(Messages.info(tr("speed.flightSpeedSet", speed)));
        }
    }

}
