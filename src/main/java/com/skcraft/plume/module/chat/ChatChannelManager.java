package com.skcraft.plume.module.chat;

import com.google.inject.Inject;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.util.module.AutoRegister;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.profile.Profiles;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@AutoRegister
public class ChatChannelManager {

    @Inject
    private ChatListener listener;
    @Inject private void init() {
        listener.manager = this;
    }

    private Map<UserId, String> users = new HashMap<>();
    private Map<String, List<UserId>> channels = new HashMap<>();

    private boolean addPlayerToChannel(String name, EntityPlayer player) {
        boolean success;
        UserId uid = Profiles.fromPlayer(player);
        if (channels.containsKey(name)) {
            success = channels.get(name).add(uid);
        } else {
            channels.put(name, new ArrayList<>());
            success = channels.get(name).add(uid);
        }
        if (success) {
            users.put(uid, name);
        }
        return success;
    }

    private boolean removePlayerFromChannel(EntityPlayer player) {
        UserId uid = Profiles.fromPlayer(player);
        String channel = users.get(uid);
        List<UserId> uidlist = channels.get(channel);
        boolean success;
        success = uidlist.remove(uid);
        if (success) {
            channels.put(channel, uidlist);
            users.remove(uid);
        }
        return success;
    }

    public void joinChannel(EntityPlayer player, String channel) {
        if (isInChatChannel(player)) {
            player.addChatMessage(Messages.info(tr("chatChannel.alreadySubscribed")));
            return;
        }

        // TODO defer
        boolean success = addPlayerToChannel(channel, player);
        player.addChatMessage(Messages.info(tr("chatChannel.join.self", channel)));
        sendMessageToChannel(channel, Messages.info(tr("chatChannel.join.other", player.getDisplayName(), channel)));
        //player.addChatMessage(Messages.error(tr("chatChannel.join.error)));
    }

    public void leaveChannel(EntityPlayer player) {
        if (!isInChatChannel(player)) {
            player.addChatMessage(Messages.info(tr("chatChannel.leave.noChannel")));
            return;
        }
        String channel = users.get(Profiles.fromPlayer(player));
        // TODO defer
        boolean success = removePlayerFromChannel(player);
        player.addChatMessage(Messages.info(tr("chatChannel.leave.self", channel)));
        sendMessageToChannel(channel, Messages.info(tr("chatChannel.leave.other", player.getDisplayName(), channel)));
        //player.addChatMessage(Messages.error(tr("chatChannel.leave.error")));
    }

    public boolean isInChatChannel(EntityPlayer player) {
        return users.containsKey(Profiles.fromPlayer(player));
    }

    @Nullable
    public String getChannel(UserId userId) {
        return users.get(userId);
    }

    @Nullable
    public String getChannel(EntityPlayer player) {
        return getChannel(Profiles.fromPlayer(player));
    }

    private static final ChatStyle CLEAR = new ChatStyle().setColor(EnumChatFormatting.RESET);
    public void sendChatToChannel(String channel, ChatComponentTranslation msg) {
        if (channels.containsKey(channel)) {
            for (UserId u : channels.get(channel)) {
                EntityPlayer player = u.getEntityPlayer();
                if (player == null) return;
                listener.sendChatMessage(player, msg);
            }
        }
    }

    public void sendMessageToChannel(String channel, IChatComponent message) {
        if (channels.containsKey(channel)) {
            for (UserId u : channels.get(channel)) {
                EntityPlayer player = u.getEntityPlayer();
                if (player == null) return;
                player.addChatComponentMessage(message);
            }
        }
    }
}
