package com.skcraft.plume.module;

import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import java.util.HashSet;
import java.util.UUID;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "status", desc = "Heal, feed and god commands")
public class Status {

    HashSet<UUID> gods = new HashSet<>();

    @Command(aliases = "heal", desc = "Regain full health points")
    @Require("plume.status.heal")
    public void heal(@Sender EntityPlayer sender) {
        sender.setHealth(sender.getMaxHealth());
        sender.addChatMessage(Messages.info(tr("status.heal")));
    }

    @Command(aliases = "feed", desc = "Regain full hunger")
    @Require("plume.status.feed")
    public void feed(@Sender EntityPlayer sender) {
        sender.getFoodStats().addStats(20, 10);
        sender.addChatMessage(Messages.info(tr("status.feed")));
    }

    @Command(aliases = "god", desc = "Toggle god mode")
    @Require("plume.status.god")
    public void god(@Sender EntityPlayer sender) {
        if (!gods.contains(sender.getUniqueID())) {
            gods.add(sender.getUniqueID());
            sender.addChatMessage(Messages.info(tr("status.god.on")));
        } else {
            gods.remove(sender.getUniqueID());
            sender.addChatMessage(Messages.info(tr("status.god.off")));
        }
    }

    @SubscribeEvent
    public void livingAttack(LivingAttackEvent event) {
        if (gods.contains(event.entityLiving.getUniqueID())) {
            event.setCanceled(true);
        }
    }
}