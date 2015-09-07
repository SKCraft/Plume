package com.skcraft.plume.module;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.skcraft.plume.common.service.auth.Authorizer;
import com.skcraft.plume.common.service.auth.Context;
import com.skcraft.plume.common.util.Environment;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.common.util.service.InjectService;
import com.skcraft.plume.common.util.service.Service;
import com.skcraft.plume.util.Contexts;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.profile.Profiles;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.CommandEvent;
import ninja.leaping.configurate.objectmapping.Setting;

import java.util.Set;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "command-filter", desc = "Configurable blocking on commands")
public class CommandFilter {

    @InjectConfig("command_filter") private Config<CommandConfig> config;
    @InjectService private Service<Authorizer> authorizer;
    @Inject private Environment environment;

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        String testCommand = event.command.getCommandName().toLowerCase();

        if (config.get().blocked.contains(testCommand)) {
            event.sender.addChatMessage(Messages.error(tr("commandFilter.blocked")));
            event.setCanceled(true);
            return;
        }

        if (config.get().consoleOnly.contains(testCommand)) {
            if (!(event.sender instanceof MinecraftServer)) {
                event.sender.addChatMessage(Messages.error(tr("commandFilter.consoleOnly")));
                event.setCanceled(true);
                return;
            }
        }

        if (config.get().restricted.contains(testCommand)) {
            if (event.sender instanceof EntityPlayer) {
                Context.Builder builder = new Context.Builder();
                environment.update(builder);
                String perm = "plume.commandfilter.restricted." + testCommand;
                Contexts.update(builder, (EntityPlayer) event.sender);

                if (!authorizer.provide().getSubject(Profiles.fromCommandSender(event.sender)).hasPermission(perm, builder.build())) {
                    event.sender.addChatMessage(Messages.error(tr("commandFilter.noPermission")));
                    event.setCanceled(true);
                    return;
                }
            } else if (!(event.sender instanceof MinecraftServer)) {
                event.sender.addChatMessage(Messages.error(tr("commandFilter.noPermission")));
                event.setCanceled(true);
            }
        }
    }

    private static class CommandConfig {
        @Setting(comment = "List of commands (in lowercase) that cannot be used")
        public Set<String> blocked = Sets.newHashSet("example_command");

        @Setting(comment = "List of commands (in lowercase) that can only be used from console")
        public Set<String> consoleOnly = Sets.newHashSet("op", "deop");

        @Setting(comment = "List of commands (in lowercase) that require the plume.commandfilter.restricted permission")
        public Set<String> restricted = Sets.newHashSet("example_command");
    }

}
