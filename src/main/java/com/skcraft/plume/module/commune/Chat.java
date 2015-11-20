package com.skcraft.plume.module.commune;

import com.skcraft.plume.module.chat.FancyName;
import cpw.mods.fml.common.FMLLog;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.java.Log;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

@Log
@Data
@EqualsAndHashCode(callSuper = true)
public class Chat extends Message {

    private String sender;
    private String message;

    @Override
    public void execute(Commune commune) {
        FancyName fancyName = commune.getFancyName();
        ChatComponentText senderText = new ChatComponentText(sender);

        if (fancyName != null) {
            EnumChatFormatting color = fancyName.nextColor(sender);
            if (color != null) {
                senderText.setChatStyle(new ChatStyle().setColor(color));
            }
        }

        IChatComponent text = new ChatComponentText("(").appendSibling(senderText).appendText(") " + message);

        FMLLog.getLogger().info("commune: (" + sender + ") " + message);

        for (Object object : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            ((EntityPlayer) object).addChatMessage(text);
        }
    }

}
