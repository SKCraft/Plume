package com.skcraft.plume.util;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
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

    public static void broadcast(IChatComponent message, Predicate<EntityPlayer> filter) {
        log.info("BROADCAST: " + message.getUnformattedText());
        for (Object object : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            EntityPlayer player = ((EntityPlayer) object);
            if (filter.apply(player)) {
                player.addChatMessage(message);
            }
        }
    }

    public static void broadcast(IChatComponent message) {
        broadcast(message, Predicates.alwaysTrue());
    }

    public static void broadcastInfo(String message) {
        broadcast(Messages.info(message));
    }

    public static void broadcastAlert(String message) {
        broadcast(Messages.error(message));
    }

    public static String toString(TileEntity tileEntity) {
        return "TileEntity: {dim=" + tileEntity.getWorld().provider.getDimensionId() +
                " pos=" + tileEntity.getPos().getX() + "," + tileEntity.getPos().getY() + "," + tileEntity.getPos().getZ() +
                " (" + tileEntity.getClass().getName() + ")}";
    }

    public static String toString(Entity entity) {
        return "Entity: {dim=" + entity.worldObj.provider.getDimensionId() +
                " pos=" + entity.posX + "," + entity.posY + "," + entity.posZ +
                " (" + entity.getClass().getName() + ")}";
    }

}
