package com.skcraft.plume.module.chat;

import com.skcraft.plume.common.util.module.AutoRegister;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import lombok.extern.java.Log;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.ServerChatEvent;

import java.util.List;

@AutoRegister
@Log
public class ChatListener {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerChat(ServerChatEvent e) {
        if (!ChatChannelManager.getManager().isInPrivateChat(e.player) || e.message.indexOf("/") == e.message.length() - 1)
            return;

        e.setCanceled(true);

        log.info("[#" + ChatChannelManager.getManager().getChannelOf(e.player) + "] " + e.username + ": " + e.message);

        ChatChannelManager.getManager().broadcast(ChatChannelManager.getManager().getChannelOf(e.player), ChatProcessor.priv(e.player.getDisplayName(), e.message));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerPublicChat(ServerChatEvent e) {
        e.setCanceled(true);

        log.info("[#global] " + e.username + ": " + e.message);

        List online = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
        for (EntityPlayerMP player : (List<EntityPlayerMP>) online) {
            // If receiver == sender AND receiver is in private chat
            if (player.getUniqueID().equals(e.player.getUniqueID()) && ChatChannelManager.getManager().isInPrivateChat(player)) {
                player.addChatMessage(e.component);
                continue;
            }

            // If receiver is in private chat
            if (ChatChannelManager.getManager().isInPrivateChat(player)) {
                player.addChatMessage(ChatProcessor.dark(e.username, e.message));
            } else {
                player.addChatMessage(e.component);
            }
        }
    }
}
