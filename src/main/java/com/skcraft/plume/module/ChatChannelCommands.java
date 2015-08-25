package com.skcraft.plume.module;

import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.module.chat.ChatChannel;
import com.skcraft.plume.module.chat.ChatChannelManager;
import com.skcraft.plume.util.Messages;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "chat-commands")
public class ChatChannelCommands {

    @Command(aliases = "join", desc = "Join a private chat channel.", usage = "/join <channel>")
    @Require("plume.chatchannels")
    public void join (@Sender ICommandSender isender, String channel) {
        if (!(isender instanceof EntityPlayerMP))
            return;

        EntityPlayerMP sender = (EntityPlayerMP) isender;

        // Check if player is already in a chat channel
        if (ChatChannelManager.getManager().isInChatChannel(sender)) {
            sender.addChatMessage(Messages.error(tr("chatchannel.err.alreadymember.other")));
            return;
        }

        // Remove any hashes
        if (channel.contains("#")) {
            channel = channel.replace("#", "");
        }

        // Find channel specified in name
        ChatChannel ch = ChatChannelManager.getManager().getChatChannelByName(channel);

        // Does channel exist already?
        if (ch != null) {
            ch.broadcastTo("§e" + sender.getDisplayName() + " §6" + tr("chatchannel.join.other") + " §e#" + ch.getName());
            ch.addMember(sender);

            sender.addChatMessage(Messages.info("§6" + tr("chatchannel.join.self") + " §e#" + ch.getName()));
        } else {
            ch = new ChatChannel(channel);

            ch.addMember(sender);

            ChatChannelManager.getManager().addChatChannel(ch);

            sender.addChatMessage(Messages.info("§6" + tr("chatchannel.join.self") + " §e#" + ch.getName()));
        }
    }

    @Command(aliases = "leave", desc = "Leave a private chat channel.", usage = "/leave")
    @Require("plume.chatchannels")
    public void leave(@Sender ICommandSender isender) {
        if (!(isender instanceof EntityPlayerMP))
            return;

        EntityPlayerMP sender = (EntityPlayerMP) isender;

        if (!ChatChannelManager.getManager().isInChatChannel(sender)) {
            sender.addChatMessage(Messages.info(tr("chatchannel.leave.nochannel")));
            return;
        }

        ChatChannel ch = ChatChannelManager.getManager().getChatChannelOf(sender);

        ch.remMember(sender);
        ch.broadcastTo("§e" + sender.getDisplayName() + " §6" + tr("chatchannel.leave.other") + " §e#" + ch.getName());

        sender.addChatMessage(Messages.info("§6" + tr("chatchannel.leave.self") + " §e#" + ch.getName()));
    }
}
