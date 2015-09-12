package com.skcraft.plume.module.perf;

import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "mob-commands", desc = "Commands to work with the number of mobs")
public class MobCommands {

    @Command(aliases = "removemobs", desc = "Kill mobs")
    @Require("plume.removemobs")
    public void removeMobs(@Sender ICommandSender sender) {
        int removed = 0;
        for (World world : MinecraftServer.getServer().worldServers) {
            for (Object object : world.loadedEntityList) {
                Entity entity = (Entity) object;
                if (entity instanceof EntityAnimal || entity instanceof EntityMob) {
                    entity.setDead();
                    removed++;
                }
            }
        }

        sender.addChatMessage(Messages.info(tr("butcher.mobsRemoved", removed)));
    }

}
