package com.skcraft.plume.module.chat;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Optional;
import com.sk89q.intake.parametric.annotation.Switch;
import com.sk89q.intake.parametric.annotation.Text;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.profile.Profiles;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraft.entity.player.EntityPlayer;
import ninja.leaping.configurate.objectmapping.Setting;

import java.util.Map;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "chat-highlighter",
        desc = "Highlights and notifies players when certain keywords appear in chat.")
public class ChatHighlighter {

    @Inject private ChatListener listener;
    @InjectConfig("chat_highlighter") private Config<HighlighterConfig> config;
    @Inject private void init() {
        listener.highlighter = this;
    }

    private Map<UserId, String[]> highlights = Maps.newHashMap();
    private Map<UserId, Boolean> sounds = Maps.newHashMap();
    private Map<UserId, String> soundNames = Maps.newHashMap();

    @Command(aliases = "highlight",
            desc = "Sets your desired keywords to highlight in chat.",
            usage = "/highlight [-s: sound.name] [keyword] [keyword2] [...]")
    @Require("plume.chathighlight")
    public void highlight(@Sender EntityPlayer sender, @Optional("") @Text String keyword) {
        String[] keywords = keyword.split(" ");
        UserId uid = Profiles.fromPlayer(sender);
        highlights.put(uid, keywords);
        if (keywords.length == 0) {
            sender.addChatMessage(Messages.info(tr("highlights.cleared")));
        } else {
            sender.addChatMessage(Messages.info(tr("highlights.set", Joiner.on(tr("listSeparator") + " ").join(keywords))));
        }
    }

    @Command(aliases = "highlightsound",
            desc = "Customizes sound notificatons for chat highlights",
            usage = "/highlightsound [-d] [sound name]")
    @Require("plume.chathighlight")
    public void highlightSound(@Sender EntityPlayer sender, @Optional("") String sound, @Switch('d') boolean toggle) {
        UserId uid = Profiles.fromPlayer(sender);
        sounds.put(uid, !toggle);
        soundNames.put(uid, (sound.isEmpty() ? config.get().soundName : sound));
        if (!toggle) {
            sender.addChatMessage(Messages.info(tr("highlights.sound.on", sound)));
        } else {
            sender.addChatMessage(Messages.info(tr("highlights.sound.off")));
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerLoggedInEvent event) {
        // TODO persistance
        UserId uid = Profiles.fromPlayer(event.player);
        highlights.put(uid, new String[] { event.player.getCommandSenderName() });
        sounds.put(uid, config.get().soundEnabled);
        soundNames.put(uid, config.get().soundName);
    }

    public String getSound(EntityPlayer recipient) {
        return soundNames.get(Profiles.fromPlayer(recipient));
    }

    public boolean isSoundEnabled(EntityPlayer recipient) {
        return sounds.get(Profiles.fromPlayer(recipient));
    }

    public String[] getKeywords(EntityPlayer recipient) {
        return highlights.get(Profiles.fromPlayer(recipient));
    }

    private static class HighlighterConfig {
        @Setting(comment = "The name of the sound to highlight users with, if enabled.")
        public String soundName = "note.pling";

        @Setting(comment = "Whether sounds should be enabled by default. (Users can change their preference with a command)")
        public boolean soundEnabled = true;
    }
}
