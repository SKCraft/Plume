package com.skcraft.plume.module.chat;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.*;

public class ChatProcessor {
    public static void sendChatMessage(EntityPlayerMP sender, String msg, EntityPlayerMP recipient) {
        String formatted;

        String[] keywords = {sender.getCommandSenderName()}; // Replace with array of highlight keywords

        formatted = ChatProcessor.highlight(msg, EnumChatFormatting.AQUA, keywords);

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
                String snippet = EnumChatFormatting.AQUA + toFormat + EnumChatFormatting.RESET;

                formatted = formatted.substring(0, start) + snippet + formatted.substring(end, formatted.length() - 1);
            }
        }

        return formatted;
    }

    public static ChatComponentText chat(String... msg) {
        String txt = "";
        for (String s : msg) {
            if (s.contains("&") && s.length() == 2) {
                txt += ChatProcessor.getColorFromCode(s).toString();
            } else txt += s;
        }

        return new ChatComponentText(txt);
    }

    public static ChatComponentText text(String msg) {
        return new ChatComponentText(msg);
    }

    public static ChatComponentText priv(String user, String msg) {
        return new ChatComponentText("<" + EnumChatFormatting.DARK_GRAY + "#" + EnumChatFormatting.RESET + user + EnumChatFormatting.RESET + "> " + msg);
    }

    public static IChatComponent dark(IChatComponent component) {
        IChatComponent dark = component.createCopy();
        dark.getChatStyle().setColor(EnumChatFormatting.DARK_GRAY);

        return dark;
    }

    public static ChatComponentText dark(String user, String msg) {
        return new ChatComponentText(EnumChatFormatting.DARK_GRAY + "<" + user + "> " + msg + EnumChatFormatting.RESET);
    }

    public static EnumChatFormatting getColorFromCode(String hex) {
        switch(hex) {
            case "&0":
                return EnumChatFormatting.BLACK;
            case "&1":
                return EnumChatFormatting.DARK_BLUE;
            case "&2":
                return EnumChatFormatting.DARK_GREEN;
            case "&3":
                return EnumChatFormatting.DARK_AQUA;
            case "&4":
                return EnumChatFormatting.DARK_RED;
            case "&5":
                return EnumChatFormatting.DARK_PURPLE;
            case "&6":
                return EnumChatFormatting.GOLD;
            case "&7":
                return EnumChatFormatting.GRAY;
            case "&8":
                return EnumChatFormatting.DARK_GRAY;
            case "&9":
                return EnumChatFormatting.BLUE;
            case "&a":
                return EnumChatFormatting.GREEN;
            case "&b":
                return EnumChatFormatting.AQUA;
            case "&c":
                return EnumChatFormatting.RED;
            case "&d":
                return EnumChatFormatting.LIGHT_PURPLE;
            case "&e":
                return EnumChatFormatting.YELLOW;
            case "&f":
                return EnumChatFormatting.WHITE;
            case "&r":
                return EnumChatFormatting.RESET;
            default:
                return EnumChatFormatting.RESET;
        }
    }
}
