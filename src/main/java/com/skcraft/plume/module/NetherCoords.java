package com.skcraft.plume.module;

import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import lombok.extern.java.Log;
import net.minecraft.entity.player.EntityPlayer;

import static com.skcraft.plume.common.util.SharedLocale.tr;


@Module(name = "nethercoords-commands")
@Log
public class NetherCoords {

    @Command(aliases = "nethercoords", desc = "Displays your corresponding nether or overworld coords")
    @Require("plume.nethercoords")
    public void nethercoords(@Sender EntityPlayer sender) {
        switch (sender.getEntityWorld().provider.dimensionId) {
            case -1: //nether
                sender.addChatMessage(Messages.info(tr("netherCoords.overworld",
                        Math.floor(sender.posX * 8), Math.floor(sender.posY), Math.floor(sender.posZ * 8))));
                break;
            case 0: //overworld
                sender.addChatMessage(Messages.info(tr("netherCoords.nether",
                        Math.floor(sender.posX / 8), Math.floor(sender.posY), Math.floor(sender.posZ / 8))));
                break;
            default: //any other
                sender.addChatMessage(Messages.info(tr("netherCoords.nether",
                        Math.floor(sender.posX / 8), Math.floor(sender.posY), Math.floor(sender.posZ / 8))));
                break;
        }
    }

}
