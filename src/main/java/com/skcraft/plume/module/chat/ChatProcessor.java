package com.skcraft.plume.module.chat;

import com.skcraft.plume.util.Messages;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.util.List;

public class ChatProcessor {
    public static void sendChatMessage(EntityPlayerMP sender, String msg, EntityPlayerMP recipient) {
        String unformatted = msg;
        String formatted;

        String[] keywords = {sender.getCommandSenderName()}; // Replace with array of highlight keywords

        formatted = ChatProcessor.highlight(unformatted, EnumChatFormatting.AQUA, keywords);

        IChatComponent component = chat(formatted);
        recipient.addChatMessage(component);
    }

    public static String highlight(String original, EnumChatFormatting color, String[] keywords) {
        String formatted = original;

        for (String key : keywords) {
            if (formatted.contains(key)) {
                int start = formatted.indexOf(key);
                int end = formatted.indexOf(" ", start);

                String toFormat = formatted.substring(start, end);
                String snippet = "§b" + toFormat + "§r";

                formatted = formatted.substring(0, start) + snippet + formatted.substring(end, formatted.length() - 1);
            }
        }

        return formatted;
    }

    public static ChatComponentText chat(String msg) {
        return new ChatComponentText(msg);
    }
}
