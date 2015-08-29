package com.skcraft.plume.module.chat;

import com.skcraft.plume.common.util.module.Module;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.event.ServerChatEvent;

import java.util.List;

@Module(name = "chatchannel-listener")
public class ChatListener {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerChat(ServerChatEvent e) {
        // Is the sender in a chat channel AND not using the override prefix?
        if (ChatChannelManager.getManager().isInChatChannel(e.player) && e.message.indexOf("\\") != 0) {
            // Then cancel the event!
            e.setCanceled(true);

            ChatChannel ch = ChatChannelManager.getManager().getChatChannelOf(e.player);

            // Broadcast function that formats with # and display name (fancy name)
            ch.sendMessage(e.player, e.message);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerPublicChat(ServerChatEvent e) {
        List<EntityPlayerMP> online = MinecraftServer.getServer().getConfigurationManager().playerEntityList;

        // Is sender NOT in a chat channel OR using the override prefix?
        if (!ChatChannelManager.getManager().isInChatChannel(e.player) || e.message.indexOf("\\") == 0) {
            e.setCanceled(true);

            ChatComponentText c;

            if (e.message.indexOf("\\") == 0)
                c = ChatProcessor.chat("§f", "<", "§r", e.player.getDisplayName(), "§f", "> " + e.message.substring(1, e.message.length()));
            else
                c = ChatProcessor.chat("§f", "<", "§r", e.player.getDisplayName(), "§f", "> " + e.message);

            for (EntityPlayerMP r : online) {
                if (r.getGameProfile().getId().equals(e.player.getGameProfile().getId())) {
                    r.addChatMessage(c);
                    return;
                }

                if (ChatChannelManager.getManager().isInChatChannel(r)) {
                    r.addChatMessage(ChatProcessor.chat("§8", c.getUnformattedText(), "§r"));
                } else {
                    r.addChatMessage(c);
                }
            }
        }
    }
}
