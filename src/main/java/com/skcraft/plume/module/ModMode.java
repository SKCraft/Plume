package com.skcraft.plume.module;

import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.PlayerData;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.DimensionManager;

import java.io.IOException;
import java.util.logging.Level;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "mod-mode", desc = "Allows moderators to enter a 'moderator mode' where their inventory and location are saved for later restore")
@Log
public class ModMode {

    @Command(aliases = "mod", desc = "Enable moderator mode")
    @Require("plume.moderator.mod")
    public void enableModMode(@Sender ICommandSender sender) {
        if (sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            PlayerData backupData = new PlayerData(DimensionManager.getCurrentSaveRootDirectory().getPath() + "/plume/temp/" + player.getGameProfile().getId().toString() + ".dat");
            PlayerData modInv = new PlayerData(DimensionManager.getCurrentSaveRootDirectory().getPath() + "/plume/modinv/" + player.getGameProfile().getId().toString() + ".dat");
            if (!backupData.getFile().exists()) {
                try {
                    backupData.load(player);
                    backupData.save();
                    modInv.load();
                    modInv.putInventory(player);
                    MinecraftServer.getServer().getConfigurationManager().addOp(player.getGameProfile());
                    player.setGameType(WorldSettings.GameType.CREATIVE);
                    player.addChatMessage(new ChatComponentText(tr("modMode.enter")));
                } catch (IOException e) {
                    player.addChatMessage(new ChatComponentText(tr("commandException")));
                    log.log(Level.WARNING, "Failed to enter mod mode", e);
                }
            } else {
                player.addChatMessage(new ChatComponentText(tr("modMode.alreadyEnabled")));
            }
        }
    }

    @Command(aliases = "done", desc = "Disable moderator mode")
    @Require("plume.moderator.done")
    public void disableModMode(@Sender ICommandSender sender) {
        if (sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            PlayerData backupData = new PlayerData(DimensionManager.getCurrentSaveRootDirectory().getPath() + "/plume/temp/" + player.getGameProfile().getId().toString() + ".dat");
            PlayerData modInv = new PlayerData(DimensionManager.getCurrentSaveRootDirectory().getPath() + "/plume/modinv/" + player.getGameProfile().getId().toString() + ".dat");
            if (backupData.getFile().exists()) {
                try {
                    modInv.load(player);
                    modInv.save();
                    backupData.load();
                    backupData.putAll(player);
                    backupData.getFile().delete();
                    MinecraftServer.getServer().getConfigurationManager().removeOp(player.getGameProfile());
                    player.fallDistance = 0;
                    player.setGameType(WorldSettings.GameType.SURVIVAL);
                } catch (IOException e) {
                    player.addChatMessage(new ChatComponentText(tr("commandException")));
                    log.log(Level.WARNING, "Failed to leave mod mode", e);
                }
            } else {
                player.addChatMessage(new ChatComponentText(tr("modMode.alreadyDisabled")));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerData backupData = new PlayerData(DimensionManager.getCurrentSaveRootDirectory().getPath() + "/plume/temp/" + event.player.getGameProfile().getId().toString() + ".dat");
        if (backupData.getFile().exists() && event.player instanceof EntityPlayerMP) {
            try {
                PlayerData modInv = new PlayerData(DimensionManager.getCurrentSaveRootDirectory().getPath() + "/plume/modinv/" + event.player.getGameProfile().getId().toString() + ".dat");
                modInv.load((EntityPlayerMP) event.player);
                modInv.save();
                backupData.load();
                backupData.putAll((EntityPlayerMP) event.player);
                backupData.getFile().delete();
                MinecraftServer.getServer().getConfigurationManager().removeOp(event.player.getGameProfile());
                event.player.setGameType(WorldSettings.GameType.SURVIVAL);
            } catch (IOException e) {
                log.log(Level.WARNING, "Failed to leave mod mode on join", e);
            }
        }
    }
}
