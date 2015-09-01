package com.skcraft.plume.module;

import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Optional;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.SharedLocale;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.event.entity.EntityEvent;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.Server;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
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

    public static EntityPlayer getTarget(ICommandSender sender, String name) {
        EntityPlayer target;

        if (name != null) {
            target = Server.findPlayer(name);

            if (target == null) {
                sender.addChatMessage(Messages.error(tr("playerNotFound", name)));
                return null;
            }
        } else {
            if (sender instanceof EntityPlayer) {
                target = (EntityPlayer) sender;
            } else {
                sender.addChatMessage(Messages.error(tr("playerRequired")));
                return null;
            }
        }

        return target;
    }

    @Command(aliases = "heal", desc = "Regain full health points")
    @Require("plume.status.heal")
    public void heal(@Sender ICommandSender sender, @Optional String name, @Optional Integer amount) {
        EntityPlayer target = getTarget(sender, name);

        if (target != null) {
            float health = amount != null ? target.getMaxHealth() + amount : 20;
            target.setHealth(health);
            target.addChatMessage(Messages.info(tr("status.heal", target.getDisplayName() + "'s")));
        }
    }

    @Command(aliases = "feed", desc = "Regain full hunger")
    @Require("plume.status.feed")
    public void feed(@Sender EntityPlayer sender, @Optional String name, @Optional Integer amount) {
        EntityPlayer target = getTarget(sender, name);

        if (target != null) {
            Integer feed = amount != null ? amount : 20;
            target.getFoodStats().addStats(feed, 10);
            target.addChatMessage(Messages.info(tr("status.feed")));
        }
    }

    @Command(aliases = "god", desc = "Enable god mode")
    @Require("plume.status.god")
    public void god(@Sender ICommandSender sender, @Optional String name) {
        EntityPlayer target = getTarget(sender, name);

        if (target != null) {
            if (!gods.contains(target.getUniqueID())) {
                gods.add(target.getUniqueID());
                sender.addChatMessage(Messages.info(tr("status.god.on", target.getDisplayName())));
            } else {
                sender.addChatMessage(Messages.error(tr("status.god.already", target.getDisplayName())));
            }
        }
    }

    @Command(aliases = "ungod", desc = "Disable god mode")
    @Require("plume.status.god")
    public void ungod(@Sender ICommandSender sender, @Optional String name) {
        EntityPlayer target = getTarget(sender, name);

        if (target != null) {
            if (gods.contains(target.getUniqueID())) {
                gods.remove(target.getUniqueID());
                sender.addChatMessage(Messages.info(tr("status.god.off", target.getDisplayName())));
            } else {
                sender.addChatMessage(Messages.error(tr("status.god.not", target.getDisplayName())));
            }
        }
    }

    @SubscribeEvent
    public void livingAttack(LivingAttackEvent event) {
        if (gods.contains(event.entityLiving.getUniqueID())) {
            event.setCanceled(true);
        }
    }
}