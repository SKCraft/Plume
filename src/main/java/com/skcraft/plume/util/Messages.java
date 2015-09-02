package com.skcraft.plume.util;

import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.util.Collection;
import java.util.logging.Level;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Log
public final class Messages {

    public static final int LINES_PER_PAGE = 10;

    private Messages() {
    }

    public static void sendMessage(ICommandSender sender, String message) {
        for (String line : message.split("\\r?\\n")) {
            sender.addChatMessage(new ChatComponentText(line));
        }
    }

    public static void sendMessages(ICommandSender sender, Collection<IChatComponent> messages) {
        for (IChatComponent line : messages) {
            sender.addChatMessage(line);
        }
    }

    public static ChatComponentText info(String message) {
        ChatComponentText msg = new ChatComponentText(message);
        msg.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        return msg;
    }

    public static ChatComponentText error(String message) {
        ChatComponentText msg = new ChatComponentText(message);
        msg.getChatStyle().setColor(EnumChatFormatting.RED);
        return msg;
    }

    public static ChatComponentText subtle(String message) {
        ChatComponentText msg = new ChatComponentText(message);
        msg.getChatStyle().setColor(EnumChatFormatting.GRAY);
        return msg;
    }

    public static ChatComponentText exception(Throwable throwable) {
        log.log(Level.SEVERE, "Error occurred during background processing", throwable);
        ChatComponentText message = new ChatComponentText(tr("commandException"));
        message.getChatStyle().setColor(EnumChatFormatting.RED);
        return message;
    }

    public static void broadcastInfo(String message) {
        log.info("BROADCAST: " + message);
        for (String name : MinecraftServer.getServer().getAllUsernames()) {
            if (name != null) {
                EntityPlayerMP player = Server.findPlayer(name);
                player.addChatMessage(Messages.info(message));
            }
        }
    }

}
