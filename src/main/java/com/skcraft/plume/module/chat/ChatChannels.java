package com.skcraft.plume.module.chat;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import net.minecraft.entity.player.EntityPlayer;

import java.util.List;
import java.util.stream.Collectors;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "chat-channels",
        desc = "Allow users to join private chat channels")
public class ChatChannels {

    @Inject
    private ChatChannelManager manager;

    @Command(aliases = "join", desc = "Join a private chat channel.", usage = "/join <channel>")
    @Require("plume.chatchannels")
    public void join(@Sender EntityPlayer sender, String channel) {
        manager.joinChannel(sender, channel);
    }

    @Command(aliases = "leave", desc = "Leave a private chat channel.", usage = "/leave")
    @Require("plume.chatchannels")
    public void leave(@Sender EntityPlayer sender) {
        manager.leaveChannel(sender);
    }

    @Command(aliases = "who", desc = "List players in a private chat channel.", usage = "/who")
    @Require("plume.chatchannels")
    public void who(@Sender EntityPlayer sender) {
        String channel = manager.getChannel(sender);
        if (channel == null) {
            // if the player themselves is in the channel this should never happen, but possibly if support is
            // added for a mod/console version which can check another channel
            sender.addChatMessage(Messages.error(tr("chatChannel.who.error")));
            return;
        }
        List<UserId> players = manager.getUsers(channel);
        if (players == null || players.isEmpty()) {
            // as above
            sender.addChatMessage(Messages.error(tr("chatChannel.who.error")));
            return;
        }
        List<String> names = players.stream().map(userId -> {
            EntityPlayer player = userId.getEntityPlayer();
            if (player == null) return userId.getName() + "*";
            else return player.getName();
        }).collect(Collectors.toList());
        sender.addChatMessage(Messages.info(tr("chatChannel.who.users", channel, Joiner.on(tr("listSeparator") + " ").join(names))));
    }
}
