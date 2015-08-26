package com.skcraft.plume.module;

import com.google.common.collect.Sets;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.CommandEvent;
import ninja.leaping.configurate.objectmapping.Setting;

import java.util.Set;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "command-filter")
public class CommandFilter {

    @InjectConfig("command-filter")
    private Config<CommandConfig> config;

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if (config.get().blocked.contains(event.command.getCommandName().toLowerCase())) {
            event.sender.addChatMessage(Messages.error(tr("commandFilter.blocked")));
            event.setCanceled(true);
            return;
        }

        if (config.get().consoleOnly.contains(event.command.getCommandName().toLowerCase())) {
            if (!(event.sender instanceof MinecraftServer)) {
                event.sender.addChatMessage(Messages.error(tr("commandFilter.consoleOnly")));
                event.setCanceled(true);
                return;
            }
        }
    }

    private static class CommandConfig {
        @Setting(comment = "List of commands (in lowercase) that cannot be used")
        public Set<String> blocked = Sets.newHashSet("example_command");

        @Setting(comment = "List of commands (in lowercase) that can only be used from console")
        public Set<String> consoleOnly = Sets.newHashSet("op", "deop");
    }

}
