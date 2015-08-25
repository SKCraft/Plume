package com.skcraft.plume.module.chat;

import com.skcraft.plume.common.UserId;
import com.skcraft.plume.util.profile.Profiles;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.HashMap;

public class ChatChannelManager {
    private HashMap<String, ChatChannel> channels = new HashMap<>();
    private HashMap<UserId, String> users = new HashMap<>();

    private static final ChatChannelManager MANAGER = new ChatChannelManager();

    public static ChatChannelManager getManager() {
        return MANAGER;
    }

    public ChatChannel getChatChannelOf(EntityPlayerMP member) {
        return this.getChatChannelByName(users.get(Profiles.fromPlayer(member)));
    }

    public ChatChannel getChatChannelByName(String name) {
        return channels.get(name.toLowerCase());
    }

    public void addTo(EntityPlayerMP player, String cc) {
        users.put(Profiles.fromPlayer(player), cc);
        channels.get(cc).addMember(player);
    }

    public void exitCC(EntityPlayerMP player) {
        channels.remove(users.get(Profiles.fromPlayer(player)));
        users.remove(Profiles.fromPlayer(player));
    }

    public void addChatChannel(ChatChannel cc) {
        channels.put(cc.getName().toLowerCase(), cc);
    }

    public void delChatChannel(String name) {
        channels.remove(name.toLowerCase());
    }

    public boolean isInChatChannel(EntityPlayerMP s) {
        return users.containsKey(Profiles.fromPlayer(s));
    }
}
