package com.skcraft.plume.module.chat;

import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.module.Module;
import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "chat-channels", desc = "Allow users to join private chat channels")
@Log
public class ChatChannels {

    @Command(aliases = "join", desc = "Join a private chat channel.", usage = "/join <channel>")
    @Require("plume.chatchannels")
    public void join (@Sender ICommandSender isender, String channel) {
        if (!(isender instanceof EntityPlayerMP))
            return;

        EntityPlayerMP sender = (EntityPlayerMP) isender;

        if (ChatChannelManager.getManager().isInPrivateChat(sender)) {
            sender.addChatMessage(ChatProcessor.text(tr("chatChannel.alreadySubscribed")));
            return;
        }

        ChatChannelManager.getManager().join(sender, channel);
    }

    @Command(aliases = "leave", desc = "Leave a private chat channel.", usage = "/leave")
    @Require("plume.chatchannels")
    public void leave(@Sender ICommandSender isender) {
        if (!(isender instanceof EntityPlayerMP))
            return;

        EntityPlayerMP sender = (EntityPlayerMP) isender;

        if (!ChatChannelManager.getManager().isInPrivateChat(sender)) {
            sender.addChatMessage(ChatProcessor.text(tr("chatChannel.leave.noChannel")));
            return;
        }

        ChatChannelManager.getManager().part(sender);
    }

    {
        log.info("ChatChannels loaded");
    }
}
