package com.skcraft.plume.module.chat;

import com.skcraft.plume.util.Messages;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;

public class ChatChannel {

    private String name;
    private ArrayList<EntityPlayerMP> members = new ArrayList<>();

    public ChatChannel(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public ArrayList<EntityPlayerMP> getMembers() {
        return this.members;
    }

    public EntityPlayerMP getMember(String name) {
        for (EntityPlayerMP m : members)
            if (m.getCommandSenderName() == name)
                return m;

        return null;
    }

    public void addMember(EntityPlayerMP newMember) {
        members.add(newMember);
    }

    public void remMember(EntityPlayerMP oldMember) {
        members.remove(oldMember);
    }

    public void broadcastTo(String msg) {
        for (EntityPlayerMP m : members)
            m.addChatMessage(Messages.info(msg));
    }

    public void sendMessage(EntityPlayerMP sender, String msg) {
        this.broadcastTo("§f<§8#§r" + sender.getDisplayName() + "§f> " + msg);
    }

    public boolean contains(EntityPlayerMP member) {
        return members.contains(member);
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
