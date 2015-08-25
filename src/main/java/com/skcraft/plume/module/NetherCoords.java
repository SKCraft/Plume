package com.skcraft.plume.module;

import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

import static com.skcraft.plume.common.util.SharedLocale.tr;


@Module(name = "nethercoords-commands")
@Log
public class NetherCoords {
    @Command(aliases = "nethercoords", desc = "Displays your corresponding nether or overworld coords")
    @Require("plume.nethercoords")
    public void nethercoords(@Sender ICommandSender sender) {
        if(sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;

            switch (player.getEntityWorld().provider.dimensionId) {
                case -1: //nether
                    sender.addChatMessage(Messages.info(tr("nethercoords.overworld",
                            Math.floor(player.posX * 8), Math.floor(player.posY), Math.floor(player.posZ * 8))));
                    break;
                case 0: //overworld
                    sender.addChatMessage(Messages.info(tr("nethercoords.nether",
                            Math.floor(player.posX / 8), Math.floor(player.posY), Math.floor(player.posZ / 8))));
                    break;
                default: //any other
                    sender.addChatMessage(Messages.info(tr("nethercoords.nether",
                            Math.floor(player.posX / 8), Math.floor(player.posY), Math.floor(player.posZ / 8))));
                    break;
            }
        } else {
            sender.addChatMessage(Messages.error(tr("messages.playerRequired")));
        }
    }

}