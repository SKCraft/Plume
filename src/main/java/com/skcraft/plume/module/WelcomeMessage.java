package com.skcraft.plume.module;

import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.VariableMessageBuilder;
import com.skcraft.plume.util.VariableMessageBuilder.MessageContext;
import com.skcraft.plume.util.concurrent.TickExecutorService;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import ninja.leaping.configurate.objectmapping.Setting;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "motd", desc = "Configurable message that shows in players' chat area when they join")
public class WelcomeMessage {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(0);

    @InjectConfig("motd") private Config<MessageConfig> config;
    @Inject private VariableMessageBuilder messageBuilder;
    @Inject private TickExecutorService tickExecutorService;

    @Command(aliases = "motd", desc = "Show the configured message of the day")
    @Require("plume.motd.motd")
    public void motd(@Sender ICommandSender sender) {
        String message = config.get().message;
        if (message != null && !message.isEmpty()) {
            sendMotd(sender);
        } else {
            sender.addChatMessage(Messages.error(tr("motd.notConfigured")));
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        String message = config.get().message;
        if (message != null && !message.isEmpty()) {
            int delay = config.get().delay;
            if (delay <= 0) {
                sendMotd(event.player);
            } else {
                EntityPlayer player = event.player;
                scheduler.schedule(() -> tickExecutorService.execute(() -> sendMotd(player)), delay, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void sendMotd(ICommandSender sender) {
        String message = config.get().message;
        if (message != null && !message.isEmpty()) {
            MessageContext context = new MessageContext();
            if (sender instanceof EntityPlayer) {
                context.setPlayer((EntityPlayer) sender);
            }
            Messages.sendMessage(sender, messageBuilder.interpolate(message, context));
        }
    }

    private static class MessageConfig {
        @Setting(comment = "The message to show in chat on join to players")
        public String message = "\u00A76Online: \u00A77({online.count})\u00A7e {online.names}";

        @Setting(comment = "The number of milliseconds to delay send of the MOTD")
        public int delay = 2000;
    }

}
