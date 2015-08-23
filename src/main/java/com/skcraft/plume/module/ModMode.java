package com.skcraft.plume.module;

import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.SharedLocale;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.PlayerData;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.DimensionManager;

import java.io.IOException;

@Module(name = "mod-mode")
@Log
public class ModMode {

    @Command(aliases = "mod", desc = "Enable moderator mode")
    @Require("plume.moderator.mod")
    public void enableModMode(@Sender ICommandSender sender) {
        if(sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            PlayerData backupData = new PlayerData(DimensionManager.getCurrentSaveRootDirectory().getPath() + "/plume/temp/" + player.getGameProfile().getId().toString() + ".dat");
            PlayerData modInv = new PlayerData(DimensionManager.getCurrentSaveRootDirectory().getPath() + "/plume/modinv/" + player.getGameProfile().getId().toString() + ".dat");
            if(!backupData.getFile().exists()) {
                try {
                    backupData.load(player);
                    backupData.save();
                    modInv.load();
                    modInv.putInventory(player);
                    MinecraftServer.getServer().getConfigurationManager().func_152605_a(player.getGameProfile());
                    player.setGameType(WorldSettings.GameType.CREATIVE);
                    player.addChatMessage(new ChatComponentText(SharedLocale.tr("modmode.enter")));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                player.addChatMessage(new ChatComponentText(SharedLocale.tr("modmode.alreadyEnabled")));
            }
        }
    }

    @Command(aliases = "done", desc = "Disable moderator mode")
    @Require("plume.moderator.done")
    public void disableModMode(@Sender ICommandSender sender) {
        if(sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            PlayerData backupData = new PlayerData(DimensionManager.getCurrentSaveRootDirectory().getPath() + "/plume/temp/" + player.getGameProfile().getId().toString() + ".dat");
            PlayerData modInv = new PlayerData(DimensionManager.getCurrentSaveRootDirectory().getPath() + "/plume/modinv/" + player.getGameProfile().getId().toString() + ".dat");
            if(backupData.getFile().exists()) {
                try {
                    modInv.load(player);
                    modInv.save();
                    backupData.load();
                    backupData.putAll(player);
                    backupData.getFile().delete();
                    MinecraftServer.getServer().getConfigurationManager().func_152610_b(player.getGameProfile());
                    player.fallDistance = 0;
                    player.setGameType(WorldSettings.GameType.SURVIVAL);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                player.addChatMessage(new ChatComponentText(SharedLocale.tr("modmode.alreadyDisabled")));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerData backupData = new PlayerData(DimensionManager.getCurrentSaveRootDirectory().getPath() + "/plume/temp/" + event.player.getGameProfile().getId().toString() + ".dat");
        if(backupData.getFile().exists() && event.player instanceof EntityPlayerMP) {
            try {
                PlayerData modInv = new PlayerData(DimensionManager.getCurrentSaveRootDirectory().getPath() + "/plume/modinv/" + event.player.getGameProfile().getId().toString() + ".dat");
                modInv.load((EntityPlayerMP) event.player);
                modInv.save();
                backupData.load();
                backupData.putAll((EntityPlayerMP) event.player);
                backupData.getFile().delete();
                MinecraftServer.getServer().getConfigurationManager().func_152610_b(event.player.getGameProfile());
                event.player.setGameType(WorldSettings.GameType.SURVIVAL);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}
