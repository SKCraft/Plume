package com.skcraft.plume.module.chat;

import com.google.inject.Inject;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.util.concurrent.Deferred;
import com.skcraft.plume.common.util.concurrent.Deferreds;
import com.skcraft.plume.common.util.module.AutoRegister;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.concurrent.BackgroundExecutor;
import com.skcraft.plume.util.concurrent.TickExecutorService;
import com.skcraft.plume.util.profile.Profiles;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@AutoRegister
public class ChatChannelManager {

    @Inject private TickExecutorService tickExecutorService;
    @Inject private BackgroundExecutor bgExecutor;
    @Inject private ChatListener listener;
    @Inject private void init() {
        listener.manager = this;
    }

    private Map<UserId, String> users = new HashMap<>();
    private Map<String, List<UserId>> channels = new HashMap<>();

    private boolean addPlayerToChannel(String name, EntityPlayer player) throws ChannelException {
        boolean success;
        UserId uid = Profiles.fromPlayer(player);
        if (users.get(uid) != null) {
            throw new AlreadyInChannelException();
        }
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

    private void removePlayerFromChannel(EntityPlayer player) throws ChannelException {
        UserId uid = Profiles.fromPlayer(player);
        String channel = users.get(uid);
        if (channel == null) {
            throw new NoChannelException();
        }
        List<UserId> uidlist = channels.get(channel);
        if (!uidlist.remove(uid)) {
            throw new ChannelException();
        }
        channels.put(channel, uidlist);
        users.remove(uid);
    }

    public void joinChannel(EntityPlayer player, String channel) {
        Deferred<?> deferred = Deferreds
                .when(() -> {
                    addPlayerToChannel(channel, player);
                    return channel;
                }, bgExecutor.getExecutor())
                .done(chanName -> {
                    sendMessageToChannel(channel, Messages.info(tr("chatChannel.join.success", player.getDisplayName(), channel)));
                }, tickExecutorService)
                .fail(e -> {
                    if (e instanceof AlreadyInChannelException) {
                        player.addChatMessage(Messages.info(tr("chatChannel.alreadySubscribed")));
                    } else if (e instanceof ChannelException) {
                        player.addChatMessage(Messages.error(tr("chatChannel.join.error")));
                    } else {
                        player.addChatMessage(Messages.exception(e));
                    }
                }, tickExecutorService);

        bgExecutor.notifyOnDelay(deferred, player);
    }

    public void leaveChannel(EntityPlayer player) {
        String channel = users.get(Profiles.fromPlayer(player));

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    removePlayerFromChannel(player);
                    return channel;
                }, bgExecutor.getExecutor())
                .done(chanName -> {
                    sendMessageToChannel(channel, Messages.info(tr("chatChannel.leave.success", player.getDisplayName(), channel)));
                }, tickExecutorService)
                .fail(e -> {
                    if (e instanceof NoChannelException) {
                        player.addChatMessage(Messages.info(tr("chatChannel.leave.noChannel")));
                    } else if (e instanceof ChannelException) {
                        player.addChatMessage(Messages.error(tr("chatChannel.leave.error")));
                    } else {
                        player.addChatMessage(Messages.exception(e));
                    }
                }, tickExecutorService);

        bgExecutor.notifyOnDelay(deferred, player);
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

    private static class ChannelException extends Exception {
    }
    private static class NoChannelException extends ChannelException {
    }
    private static class AlreadyInChannelException extends ChannelException {
    }
}
