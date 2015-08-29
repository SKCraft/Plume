package com.skcraft.plume.module.chat;

import com.skcraft.plume.common.UserId;
import com.skcraft.plume.util.Server;
import com.skcraft.plume.util.profile.Profiles;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.UUID;

public class ChatChannel {

    private String name;
    private ArrayList<UserId> members = new ArrayList<>();

    public ChatChannel(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name.toLowerCase();
    }

    public ArrayList<UserId> getMembers() {
        return this.members;
    }

    public void addMember(EntityPlayerMP newMember) {
        members.add(Profiles.fromPlayer(newMember));
    }

    public void remMember(EntityPlayerMP oldMember) {
        members.remove(Profiles.fromPlayer(oldMember));

        if (members.isEmpty()) {
            ChatChannelManager.getManager().delChatChannel(this.getName());
        }
    }

    public void broadcastTo(String... msg) {
        for (UserId uid : getMembers()) {
            EntityPlayerMP player = Server.findPlayer(uid.getUuid());

            if (player != null)
                player.addChatMessage(ChatProcessor.chat(msg));
        }
    }

    public void sendMessage(EntityPlayerMP sender, String msg) {
        this.broadcastTo("§f", "<", "§8", "#", "§r", sender.getDisplayName(), "§f", "> " + msg);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ChatChannel))
            return false;

        ChatChannel otherObj = (ChatChannel) obj;

        if (this.getName().equalsIgnoreCase(otherObj.getName()))
            return true;
        else
            return false;
    }
}
