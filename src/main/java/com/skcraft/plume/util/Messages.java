package com.skcraft.plume.util;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public final class Messages {

    private Messages() {
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

}
