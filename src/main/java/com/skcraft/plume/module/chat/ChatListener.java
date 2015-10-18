package com.skcraft.plume.module.chat;

import com.skcraft.plume.common.util.module.AutoRegister;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import lombok.extern.java.Log;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentStyle;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.event.ServerChatEvent;

import java.util.List;

@AutoRegister
@Log
public class ChatListener {

    protected ChatChannelManager manager;
    protected ChatHighlighter highlighter;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerChat(ServerChatEvent e) {
        if (!(manager != null && manager.isInChatChannel(e.player)) || e.message.endsWith("/")) {
            return;
        }

        e.setCanceled(true);

        String channel = manager.getChannel(e.player);
        log.info("[#" + channel + "] " + e.username + ": " + e.message);

        manager.sendChatToChannel(channel, e.component);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerPublicChat(ServerChatEvent e) {
        e.setCanceled(true);

        log.info("[#] " + e.username + ": " + e.message);

        @SuppressWarnings("unchecked")
        List<EntityPlayer> online = (List<EntityPlayer>) MinecraftServer.getServer().getConfigurationManager().playerEntityList;
        for (EntityPlayer recipient : online) {
            if (!recipient.getUniqueID().equals(e.player.getUniqueID())) {
                ChatComponentTranslation formatted = e.component.createCopy();
                if (manager != null && manager.isInChatChannel(recipient)) {
                    formatted = darken(formatted);
                }
                if (highlighter != null) {
                    formatted = highlight(formatted, highlighter.getKeywords(recipient), colorFromName(recipient));
                }
                sendChatMessage(recipient, formatted);
            } else {
                sendChatMessage(recipient, e.component);
            }
        }
    }

    private static EnumChatFormatting colorFromName(EntityPlayer player) {
        if (player.getDisplayName().matches("^\u00a7[0-9A-Fa-f].*")) {
            char control = player.getDisplayName().charAt(1);
            for (EnumChatFormatting col : EnumChatFormatting.values()) {
                if (col.getFormattingCode() == control) {
                    return col;
                }
            }
        }
        return EnumChatFormatting.RED; // default highlight color
    }

    public void sendChatMessage(EntityPlayer recipient, ChatComponentTranslation msg) {
        recipient.addChatMessage(msg);
    }

    @SuppressWarnings("unchecked")
    private static ChatComponentTranslation highlight(ChatComponentTranslation original, String[] keywords, EnumChatFormatting color) {
        if (keywords == null || keywords.length == 0) return original; // nothing to do
        ChatStyle origStyle = original.getChatStyle();
        Object[] args = original.getFormatArgs();
        Object[] copyArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof IChatComponent) {
                IChatComponent sub = ((IChatComponent) args[i]);
                ChatStyle subStyle = sub.getChatStyle();
                IChatComponent base = new ChatComponentText("");
                String unformatted = sub.getUnformattedText();
                // TODO allow multiple match highlighting
                for (String keyword : keywords) {

                    if (unformatted.contains(keyword)) {
                        String[] split = unformatted.split(keyword);


                        if (split.length > 0) {
                            IChatComponent before = new ChatComponentText(split[0]);
                            before.setChatStyle(subStyle);
                            base.appendSibling(before);
                        }

                        IChatComponent highlight = new ChatComponentText(keyword);
                        highlight.setChatStyle(subStyle.createShallowCopy());
                        highlight.getChatStyle().setColor(color);
                        base.appendSibling(highlight);

                        if (split.length > 1) {
                            IChatComponent after = new ChatComponentText(split[1]);
                            after.setChatStyle(subStyle);
                            base.appendSibling(after);
                        }

                        base.setChatStyle(subStyle);
                        copyArgs[i] = base;
                        break;
                    } else {
                        copyArgs[i] = sub;
                    }
                }
            } else {
                copyArgs[i] = args[i];
            }
        }
        ChatComponentTranslation highlighted = new ChatComponentTranslation(original.getKey(), copyArgs);
        highlighted.setChatStyle(origStyle);
        return highlighted;
    }

    @SuppressWarnings("unchecked")
    public static ChatComponentTranslation darken(ChatComponentTranslation msg) {
        Object[] args = msg.getFormatArgs();
        Object[] copyArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof IChatComponent) {
                IChatComponent sub = ((IChatComponent) args[i]).createCopy();
                if (sub.getUnformattedText().matches("^\u00a7[0-9A-Fa-f].*")) {
                    ChatComponentStyle subColored = new ChatComponentText(sub.getUnformattedText().substring(2));
                    subColored.setChatStyle(sub.getChatStyle());
                    subColored.getChatStyle().setColor(EnumChatFormatting.DARK_GRAY);
                    copyArgs[i] = subColored;
                } else {
                    sub.getChatStyle().setColor(EnumChatFormatting.DARK_GRAY);
                    copyArgs[i] = sub;
                }
            } else {
                copyArgs[i] = args[i];
            }
        }
        ChatComponentTranslation color = new ChatComponentTranslation(msg.getKey(), copyArgs);
        color.getChatStyle().setColor(EnumChatFormatting.DARK_GRAY);
        return color;
    }
}
