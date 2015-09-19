package com.skcraft.plume.module.player;

import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Optional;
import com.sk89q.intake.parametric.annotation.Switch;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.SharedLocale;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.event.entity.EntityEvent;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.Server;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import javax.swing.text.html.parser.Entity;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "status", desc = "Heal, feed and god commands")
public class Status {

    private final Set<UUID> gods = new HashSet<>();

    @Command(aliases = "heal", desc = "Regain full health points")
    @Require("plume.status.heal")
    public void heal(@Sender ICommandSender sender, @Optional EntityPlayerMP target, @Switch('a') Integer amount) {
        if (!(sender instanceof EntityPlayerMP) && target == null) {
            sender.addChatMessage(Messages.error(tr("playerRequired")));
            return;
        }

        EntityPlayerMP player = target != null ? target : (EntityPlayerMP) sender;
        float health = amount != null ? player.getHealth() + amount : player.getMaxHealth();

        player.setHealth(health);
        player.addChatMessage(Messages.info(tr("status.heal")));
    }

    @Command(aliases = "feed", desc = "Regain full hunger")
    @Require("plume.status.feed")
    public void feed(@Sender ICommandSender sender, @Optional EntityPlayerMP target, @Switch('a') Integer amount) {
        if (!(sender instanceof EntityPlayerMP) && target == null) {
            sender.addChatMessage(Messages.error(tr("playerRequired")));
            return;
        }

        EntityPlayerMP player = target != null ? target : (EntityPlayerMP) sender;
        Integer food = amount != null ? amount : 20;

        player.getFoodStats().addStats(food, 10);
        player.addChatMessage(Messages.info(tr("status.feed")));
    }

    @Command(aliases = "god", desc = "Enable god mode")
    @Require("plume.status.god")
    public void god(@Sender ICommandSender sender, @Optional EntityPlayerMP target) {
        if (!(sender instanceof EntityPlayerMP) && target == null) {
            sender.addChatMessage(Messages.error(tr("playerRequired")));
            return;
        }

        EntityPlayerMP player = target != null ? target : (EntityPlayerMP) sender;

        if (!gods.contains(player.getUniqueID())) {
            gods.add(player.getUniqueID());
            sender.addChatMessage(Messages.info(tr("status.god.on", player.getDisplayName())));
        } else {
            sender.addChatMessage(Messages.error(tr("status.god.already", player.getDisplayName())));
        }
    }

    @Command(aliases = "ungod", desc = "Disable god mode")
    @Require("plume.status.god")
    public void ungod(@Sender ICommandSender sender, @Optional EntityPlayerMP target) {
        if (!(sender instanceof EntityPlayerMP) && target == null) {
            sender.addChatMessage(Messages.error(tr("playerRequired")));
            return;
        }

        EntityPlayerMP player = target != null ? target : (EntityPlayerMP) sender;

        if (gods.contains(player.getUniqueID())) {
            gods.remove(player.getUniqueID());
            sender.addChatMessage(Messages.info(tr("status.god.off", player.getDisplayName())));
        } else {
            sender.addChatMessage(Messages.error(tr("status.god.not", player.getDisplayName())));
        }
    }

    @SubscribeEvent
    public void livingAttack(LivingAttackEvent event) {
        if (gods.contains(event.entityLiving.getUniqueID())) {
            event.setCanceled(true);
        }
    }
}
