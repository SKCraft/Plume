package com.skcraft.plume.module.chat;

import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.ServerChatEvent;

import java.util.List;

@Module(name = "chatchannel-listener")
public class ChatListener {
    @SubscribeEvent
    public void onPlayerChat(ServerChatEvent e) {
        e.setCanceled(true);

        // Is the sender in a chat channel AND not using the override prefix?
        if (ChatChannelManager.getManager().isInChatChannel(e.player) && e.message.indexOf("\\") != 0) {
            ChatChannel ch = ChatChannelManager.getManager().getChatChannelOf(e.player);

            // Broadcast function that formats with # and display name (fancy name)
            ch.sendMessage(e.player, e.message);
        } else {
            List<EntityPlayerMP> online = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
            for (EntityPlayerMP r : online) {
                if (r.getCommandSenderName() == e.player.getCommandSenderName()) {
                    r.addChatMessage(Messages.info(e.component.getFormattedText()));
                }

                if (ChatChannelManager.getManager().isInChatChannel(r)) {
                    r.addChatMessage(Messages.info("§8" + e.component.getUnformattedText() + "§r"));
                } else {
                    r.addChatMessage(Messages.info(e.component.getFormattedText()));
                }
            }
        }
    }

    /* Do I need to cleanse players on leave, or are they re-added on rejoin? Either way, need a timer...
    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (ChatChannelManager.getManager().isInChatChannel((EntityPlayerMP) event.player)) {

        }
    }
    */
}
