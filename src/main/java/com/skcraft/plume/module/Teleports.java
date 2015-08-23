package com.skcraft.plume.module;

import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.SharedLocale;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.TeleportHelper;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

@Module(name = "teleports")
public class Teleports {

    @Command(aliases = "to", desc = "Teleport to the player specified")
    @Require("plume.teleport.to")
    public void to(@Sender ICommandSender sender, String name) {
        if(sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            EntityPlayerMP target = MinecraftServer.getServer().getConfigurationManager().func_152612_a(name);
            if (target != null) {
                TeleportHelper.teleport(player, target);
                sender.addChatMessage(new ChatComponentText(SharedLocale.tr("teleport.success")));
            } else {
                sender.addChatMessage(new ChatComponentText(SharedLocale.tr("teleport.playerNotFound", name)));
            }
        } else {
            sender.addChatMessage(new ChatComponentText(SharedLocale.tr("messages.playerRequired")));
        }
    }

    @Command(aliases = "bring", desc = "Brings a player to you")
    @Require("plume.teleport.bring")
    public void bring(@Sender ICommandSender sender, String name) {
        if(sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            EntityPlayerMP target = MinecraftServer.getServer().getConfigurationManager().func_152612_a(name);
            if (target != null) {
                TeleportHelper.teleport(target, player);
                sender.addChatMessage(new ChatComponentText(SharedLocale.tr("teleport.success")));
            } else {
                sender.addChatMessage(new ChatComponentText(SharedLocale.tr("teleport.playerNotFound", name)));
            }
        } else {
            sender.addChatMessage(new ChatComponentText(SharedLocale.tr("messages.playerRequired")));
        }
    }
}
