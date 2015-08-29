package com.skcraft.plume.module;

import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.module.border.WorldBorderConfig;
import com.skcraft.plume.module.chat.ChatProcessor;
import com.skcraft.plume.util.Location3d;
import com.skcraft.plume.util.Messages;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.player.EntityPlayer;

import java.util.HashSet;
import java.util.UUID;

@Module(name = "world-border", desc = "Configurable world border that warns and then bounces players back from it.")
public class WorldBorder {
    @InjectConfig("worldborder") private Config<WorldBorderConfig> config;

    HashSet<UUID> buffered = new HashSet<>();

    @SubscribeEvent
    public void playerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer p = event.player;
        WorldBorderConfig.Border b = config.get().border;

        if (Math.floor(p.posX) > Math.floor(p.prevPosX) ||
                Math.floor(p.posX) < Math.floor(p.prevPosX) ||
                Math.floor(p.posZ) > Math.floor(p.prevPosZ) || Math.floor(p.posZ) < Math.floor(p.prevPosZ)) {
            WorldBorderConfig.Border.Threshold t = b.borderType.getThreshold(Location3d.fromEntity(p), b.centre.getLocation(), b.borderSize, b.bufferSize);

            if (t == WorldBorderConfig.Border.Threshold.CLEAR) {
                buffered.remove(p.getUniqueID());
            } else if (t == WorldBorderConfig.Border.Threshold.BUFFER && !buffered.contains(p.getUniqueID())) {
                p.addChatMessage(Messages.info("Careful! You're near the border."));
                buffered.add(p.getUniqueID());
            } else if (t == WorldBorderConfig.Border.Threshold.BORDER) {
                p.setVelocity(Math.floor(p.posX) - Math.floor(p.prevPosX), p.posY + 0.3, Math.floor(p.posZ) - Math.floor(p.prevPosZ));
            }
        }
    }

    @SubscribeEvent
    public void playerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (buffered.contains(event.player.getUniqueID()))
            buffered.remove(event.player.getUniqueID());
    }
}
