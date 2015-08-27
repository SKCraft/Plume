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
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import ninja.leaping.configurate.objectmapping.Setting;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "motd")
public class WelcomeMessage {

    @InjectConfig("motd") private Config<MessageConfig> config;
    @Inject private VariableMessageBuilder messageBuilder;

    @Command(aliases = "motd", desc = "Show the configured message of the day")
    @Require("plume.motd.motd")
    public void motd(@Sender ICommandSender sender) {
        String message = config.get().message;
        if (message != null && !message.isEmpty()) {
            MessageContext context = new MessageContext();
            if (sender instanceof EntityPlayer) {
                context.setPlayer((EntityPlayer) sender);
            }
            Messages.sendMessage(sender, messageBuilder.interpolate(message, context));
        } else {
            sender.addChatMessage(Messages.error(tr("motd.notConfigured")));
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        String message = config.get().message;
        if (message != null && !message.isEmpty()) {
            MessageContext context = new MessageContext().setPlayer(event.player);
            Messages.sendMessage(event.player, messageBuilder.interpolate(message, context));
        }
    }

    private static class MessageConfig {
        @Setting(comment = "The message to show in chat on join to players")
        public String message = "\u00A76Online: \u00A77({online.count})\u00A7e {online.names}";
    }

}
