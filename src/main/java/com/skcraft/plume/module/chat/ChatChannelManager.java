package com.skcraft.plume.module.chat;

import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;

public class ChatChannelManager {
    private ArrayList<ChatChannel> channels = new ArrayList<ChatChannel>();

    private static final ChatChannelManager MANAGER = new ChatChannelManager();

    public static ChatChannelManager getManager() {
        return MANAGER;
    }

    public ChatChannel getChatChannelOf(EntityPlayerMP member) {
        for (ChatChannel ch : channels)
            if (ch.contains(member))
                return ch;

        return null;
    }

    public ChatChannel getChatChannelByName(String name) {
        for (ChatChannel ch : channels)
            if (ch.getName() == name)
                return ch;

        return null;
    }

    public void addChatChannel(ChatChannel cc) {
        channels.add(cc);
    }

    public void delChatChannel(ChatChannel cc) {
        channels.remove(cc);
    }

    public boolean isInChatChannel(EntityPlayerMP s) {
        if (this.getChatChannelOf(s) != null)
            return true;
        else
            return false;
    }
}
