package com.skcraft.plume.module.chat;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Optional;
import com.sk89q.intake.parametric.annotation.Switch;
import com.sk89q.intake.parametric.annotation.Text;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.DataDir;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.profile.Profiles;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraft.entity.player.EntityPlayer;
import ninja.leaping.configurate.objectmapping.Setting;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "chat-highlighter",
        desc = "Highlights and notifies players when certain keywords appear in chat.")
public class ChatHighlighter {

    @Inject @DataDir private File plumeDir;
    private File highlightDir;
    @Inject private ChatListener listener;
    @InjectConfig("chat_highlighter") private Config<HighlighterConfig> config;
    @Inject private void init() {
        listener.highlighter = this;
        highlightDir = new File(plumeDir, "chathighlights" + File.separator);
        highlightDir.mkdirs();
    }

    private Map<UserId, String[]> highlights = Maps.newHashMap();
    private Map<UserId, Boolean> sounds = Maps.newHashMap();
    private Map<UserId, String> soundNames = Maps.newHashMap();

    @Command(aliases = "highlight",
            desc = "Sets your desired keywords to highlight in chat.",
            usage = "/highlight [keyword] [keyword2] [...]")
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
        writeFile(new File(highlightDir, uid.getUuid().toString()), isSoundEnabled(sender),
                getSound(sender), keywords.length > 0 ? Joiner.on(' ').join(keywords) : "");
    }

    @Command(aliases = "highlightsound",
            desc = "Customizes sound notificatons for chat highlights",
            usage = "/highlightsound [-d] [sound name]",
    help = "/highlightsound [-d] [sound name]\n" +
           " -d flag disables sound\n" +
           " Specifying a sound name will set that sound to your highlight.\n" +
           " If you omit it it will use the default sound.")
    @Require("plume.chathighlight")
    public void highlightSound(@Sender EntityPlayer sender, @Optional("") String sound, @Switch('d') boolean toggle) {
        UserId uid = Profiles.fromPlayer(sender);
        boolean enabled = !toggle || !sound.isEmpty();
        sounds.put(uid, enabled);
        String soundName = sound.isEmpty() ? config.get().soundName : sound;
        soundNames.put(uid, soundName);
        if (!toggle) {
            sender.addChatMessage(Messages.info(tr("highlights.sound.on", soundName)));
        } else {
            sender.addChatMessage(Messages.info(tr("highlights.sound.off")));
        }
        writeFile(new File(highlightDir, uid.getUuid().toString()), enabled, soundName, Joiner.on(' ').join(getKeywords(sender)));
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerLoggedInEvent event) {
        UserId uid = Profiles.fromPlayer(event.player);

        File userFile = new File(highlightDir, uid.getUuid().toString());
        boolean enabled = config.get().soundEnabled;
        String sound =  config.get().soundName;
        String keywords = event.player.getName();
        if (!userFile.exists()) {
            writeFile(userFile, enabled, sound, keywords);
        } else {
            try {
                List<String> lines = Files.readLines(userFile, Charset.forName("UTF-8"));
                enabled = Boolean.valueOf(lines.get(0));
                sound = lines.get(1);
                keywords = lines.get(2);
            } catch (IOException ignored) {
            }
        }
        highlights.put(uid, keywords.split(" "));
        sounds.put(uid, enabled);
        soundNames.put(uid, sound);
    }

    private static void writeFile(File file, boolean enabled, String sound, String keywords) {
        try {
            if (!file.exists()) file.createNewFile();
            String data = enabled + "\n" + sound + "\n" + keywords;
            Files.write(data, file, Charset.forName("UTF-8"));
        } catch (IOException ignored) { // settings just won't persist
        }
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
