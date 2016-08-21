package com.skcraft.plume.module.chat;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.skcraft.plume.common.util.logging.Log4jRedirect;
import com.skcraft.plume.common.util.module.AutoRegister;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import lombok.extern.java.Log;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentStyle;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.event.ServerChatEvent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AutoRegister
@Log
public class ChatListener {

    protected ChatChannelManager manager;
    protected ChatHighlighter highlighter;

    @Inject
    void init() {
        log.setUseParentHandlers(false);
        log.addHandler(new Log4jRedirect(FMLLog.getLogger(), "chat"));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerChat(ServerChatEvent e) {
        if (!(manager != null && manager.isInChatChannel(e.player)) || e.message.endsWith("/")) {
            return;
        }

        e.setCanceled(true);

        String channel = manager.getChannel(e.player);
        log.info("[#" + channel + "] <" + e.username + "> " + e.message);

        manager.sendChatToChannel(channel, e.component);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerPublicChat(ServerChatEvent e) {
        e.setCanceled(true);

        log.info("[#] <" + e.username + "> " + e.message);

        @SuppressWarnings("unchecked")
        List<EntityPlayerMP> online = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
        for (EntityPlayer recipient : online) {
            if (!recipient.getUniqueID().equals(e.player.getUniqueID())) {
                sendChatMessage(recipient, this.format(e.component, recipient, true, true));
            } else {
                sendChatMessage(recipient, e.component);
            }
        }
    }

    ChatComponentTranslation format(ChatComponentTranslation component, EntityPlayer recipient, boolean tryDarken, boolean tryHighlight) {
        ChatComponentTranslation formatted = component.createCopy();
        if (tryDarken && manager != null && manager.isInChatChannel(recipient)) {
            formatted = darken(formatted);
        }
        if (tryHighlight && highlighter != null) {
            formatted = highlight(formatted, highlighter.getKeywords(recipient),
                    colorFromName(recipient), highlighter.isSoundEnabled(recipient) ? recipient : null);
        }
        return formatted;
    }

    private static EnumChatFormatting colorFromName(EntityPlayer player) {
        if (player.getName().matches("^\u00a7[0-9A-Fa-f].*")) {
            char control = player.getName().charAt(1);
            for (EnumChatFormatting col : EnumChatFormatting.values()) {
                if (col.toString().equals("\u00a7" + control)) {
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
    private ChatComponentTranslation highlight(ChatComponentTranslation original, String[] keywords,
                                                      EnumChatFormatting color, EntityPlayer playSoundTo) {
        if (keywords == null || keywords.length == 0) return original; // nothing to do
        ChatStyle origStyle = original.getChatStyle();
        Object[] args = original.getFormatArgs();
        Object[] copyArgs = new Object[args.length];
        boolean found = false;
        Pattern pattern = Pattern.compile("(" + Joiner.on('|').join(keywords) + ")", Pattern.CASE_INSENSITIVE);
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof IChatComponent) {
                IChatComponent sub = ((IChatComponent) args[i]);
                ChatStyle subStyle = sub.getChatStyle();
                IChatComponent base = new ChatComponentText("");
                String unformatted = sub.getUnformattedText();
                Matcher matcher = pattern.matcher(unformatted);
                int prevEnd = 0;
                while (matcher.find()) {
                    found = true;
                    String match = matcher.group();
                    IChatComponent before = new ChatComponentText(
                            unformatted.substring(prevEnd, matcher.start()));
                    prevEnd = matcher.end();
                    before.setChatStyle(subStyle);
                    base.appendSibling(before);
                    IChatComponent highlight = new ChatComponentText(match);
                    highlight.setChatStyle(subStyle.createShallowCopy());
                    highlight.getChatStyle().setColor(color);
                    base.appendSibling(highlight);
                }
                if (prevEnd != 0) {
                    IChatComponent after = new ChatComponentText(
                            unformatted.substring(prevEnd, unformatted.length()));
                    after.setChatStyle(subStyle);
                    base.appendSibling(after);
                }

                base.setChatStyle(subStyle);
                copyArgs[i] = found ? base : sub;
            } else {
                copyArgs[i] = args[i];
            }
        }
        if (found && playSoundTo != null) {
            if (playSoundTo instanceof EntityPlayerMP) {
                EntityPlayerMP playerMP = ((EntityPlayerMP) playSoundTo);
                S29PacketSoundEffect packet29 = new S29PacketSoundEffect(
                        highlighter.getSound(playerMP), playerMP.posX, playerMP.posY, playerMP.posZ, 1.0f, 1.0f);
                playerMP.playerNetServerHandler.sendPacket(packet29);
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
