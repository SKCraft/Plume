package com.skcraft.plume.util;

import lombok.extern.java.Log;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.logging.Level;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Log
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

    public static ChatComponentText exception(Throwable throwable) {
        log.log(Level.SEVERE, "Error occurred during background processing", throwable);
        ChatComponentText message = new ChatComponentText(tr("messages.exception"));
        message.getChatStyle().setColor(EnumChatFormatting.RED);
        return message;
    }

}
