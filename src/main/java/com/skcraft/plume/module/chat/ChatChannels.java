package com.skcraft.plume.module.chat;

import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.module.Module;
import net.minecraft.entity.player.EntityPlayer;

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
}
