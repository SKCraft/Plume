package com.skcraft.plume.module.chat;

import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.util.module.AutoRegister;
import com.skcraft.plume.util.profile.Profiles;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IChatComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@AutoRegister
public class ChatChannelManager {
    private static final ChatChannelManager INSTANCE = new ChatChannelManager();

    public static ChatChannelManager getManager() {
        return INSTANCE;
    }

    private HashMap<UserId, String> users = new HashMap<>();
    private HashMap<String, List<UserId>> channels = new HashMap<>();

    private void cc(String name, EntityPlayerMP player) {
        if (channels.containsKey(name))
            channels.get(name).add(Profiles.fromPlayer(player));
        else {
            channels.put(name, new ArrayList<>());
            channels.get(name).add(Profiles.fromPlayer(player));
        }
    }

    private void ccp(EntityPlayerMP player, String name) {
        if (channels.containsKey(name))
            channels.get(name).remove(Profiles.fromPlayer(player));
    }

    private void cp(EntityPlayerMP player) {
        this.ccp(player, users.get(Profiles.fromPlayer(player)));
    }

    public void join(EntityPlayerMP player, String channel) {
        player.addChatMessage(ChatProcessor.chat("&e", tr("chatChannel.join.self"), "&6", " #", channel));
        this.broadcast(channel,
                ChatProcessor.chat(player.getDisplayName(), " ", "&e", tr("chatChannel.join.other"), "&6", " #", channel));

        users.put(Profiles.fromPlayer(player), channel.replace("#", ""));
        this.cc(channel, player);
    }

    public void part(EntityPlayerMP player) {
        String channel = users.get(Profiles.fromPlayer(player));

        player.addChatMessage(ChatProcessor.chat("&e", tr("chatChannel.leave.self"), "&6", " #", channel));

        this.cp(player);
        this.broadcast(channel,
                ChatProcessor.chat(player.getDisplayName(), " ", "&e", tr("chatChannel.leave.other"), "&6", " #", channel));

        users.remove(Profiles.fromPlayer(player));
    }

    public boolean isInPrivateChat(EntityPlayerMP player) {
        return users.containsKey(Profiles.fromPlayer(player));
    }

    public String getChannelOf(UserId u) {
        return users.get(u);
    }

    public String getChannelOf(EntityPlayerMP player) {
        return this.getChannelOf(Profiles.fromPlayer(player));
    }

    public void broadcast(String channel, IChatComponent msg) {
        if (channels.containsKey(channel))
            for (UserId u : channels.get(channel)) {
                if (u.getPlayerOfThis() == null) return;

                u.getPlayerOfThis().addChatMessage(msg);
            }
    }
}
