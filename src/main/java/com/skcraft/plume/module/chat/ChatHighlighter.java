package com.skcraft.plume.module.chat;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Optional;
import com.sk89q.intake.parametric.annotation.Text;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.profile.Profiles;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Map;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "chat-highlighter",
        desc = "Highlights and notifies players when certain keywords appear in chat.")
public class ChatHighlighter {

    @Inject
    private ChatListener listener;
    @Inject private void init() {
        listener.highlighter = this;
    }

    private Map<UserId, String[]> highlights = Maps.newHashMap();

    @Command(aliases = "highlight", desc = "Sets your desired keywords to highlight in chat.", usage = "/highlight [keyword] [keyword2] [...]")
    @Require("plume.chathighlight")
    public void highlight(@Sender EntityPlayer sender, @Optional @Text String keyword) {
        String[] keywords = keyword.split(" ");
        highlights.put(Profiles.fromPlayer(sender), keywords);
        if (keywords.length == 0) {
            sender.addChatMessage(Messages.info(tr("highlights.cleared")));
        } else {
            sender.addChatMessage(Messages.info(tr("highlights.set", Joiner.on(tr("listSeparator") + " ").join(keywords))));
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerLoggedInEvent event) {
        // TODO serialize player-set highlights to persist over server restarts
        highlights.put(Profiles.fromPlayer(event.player), new String[] { event.player.getCommandSenderName() });
    }

    public String[] getKeywords(EntityPlayer recipient) {
        return highlights.get(Profiles.fromPlayer(recipient));
    }
}
