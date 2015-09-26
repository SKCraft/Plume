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
        if (!ChatChannelManager.getManager().isInPrivateChat(e.player))
            return;

        e.setCanceled(true);

        ChatChannelManager.getManager().broadcast(ChatChannelManager.getManager().getChannelOf(e.player), ChatProcessor.priv(e.username, e.message));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerPublicChat(ServerChatEvent e) {
        if (!ChatChannelManager.getManager().isInPrivateChat(e.player))
            return;

        e.setCanceled(true);

        List online = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
        for (EntityPlayerMP player : (List<EntityPlayerMP>) online) {
            if (player.getUniqueID().equals(e.player.getUniqueID()))
                continue;

            if (ChatChannelManager.getManager().isInPrivateChat(player)) {
                player.addChatMessage(ChatProcessor.dark(e.component.getUnformattedTextForChat()));
            } else {
                player.addChatMessage(e.component);
            }
        }
    }

    {
        log.info("ChatListener overriding chat");
    }
}
