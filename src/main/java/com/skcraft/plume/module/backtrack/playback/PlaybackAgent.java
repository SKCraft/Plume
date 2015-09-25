package com.skcraft.plume.module.backtrack.playback;

import com.google.inject.Inject;
import com.skcraft.plume.common.service.journal.Record;
import com.skcraft.plume.common.util.Cursor;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.AutoRegister;
import com.skcraft.plume.module.backtrack.action.Action;
import com.skcraft.plume.module.backtrack.ActionMap;
import com.skcraft.plume.module.backtrack.ActionReadException;
import com.skcraft.plume.module.backtrack.LoggerConfig;
import com.skcraft.plume.util.Messages;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import lombok.Getter;
import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;

import java.io.IOException;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.skcraft.plume.common.util.SharedLocale.tr;

@AutoRegister
@Log
public class PlaybackAgent {

    private static final int MAX_CONCURRENT_PLAYBACKS = 1;

    @InjectConfig("backtrack") private Config<LoggerConfig> config;
    @Inject private ActionMap actionMap;
    private final Queue<Playback> pending = new LinkedBlockingQueue<>(MAX_CONCURRENT_PLAYBACKS);

    public void addPlayback(Cursor<Record> cursor, ICommandSender sender, PlaybackType playbackType) throws RejectedPlaybackException {
        checkNotNull(cursor, "cursor");
        checkNotNull(sender, "sender");
        checkNotNull(playbackType, "playType");
        try {
            pending.add(new Playback(cursor, sender, playbackType));
        } catch (IllegalStateException e) {
            throw new RejectedPlaybackException();
        }
    }

    @SubscribeEvent
    public void tickStart(TickEvent event) {
        if (event.side != Side.SERVER) return;

        synchronized (this) {
            long start = System.currentTimeMillis();
            long maxTimePerTick = config.get().playback.maxTimePerTick;

            if (pending.isEmpty()) {
                return;
            }

            do {
                Iterator<Playback> it = pending.iterator();
                while (it.hasNext()) {
                    Playback playback = it.next();
                    Cursor<Record> cursor = playback.getCursor();

                    if (cursor.hasNext()) {
                        Record record = cursor.next();
                        try {
                            Action action = actionMap.readRecord(record);
                            playback.getPlaybackType().apply(action, record);
                            playback.onApply();
                        } catch (ActionReadException e) {
                            log.log(Level.WARNING, "Failed to parse action " + record, e);
                        } catch (Throwable t) {
                            log.log(Level.WARNING, "Failed to play back " + record, t);
                        }
                    } else {
                        it.remove();
                        playback.getSender().addChatMessage(Messages.info(tr("logger.playback.finished", playback.getCount())));
                        try {
                            cursor.close();
                        } catch (IOException e) {
                            log.log(Level.WARNING, "Couldn't close cursor after finishing playback", e);
                        }
                    }
                }
            } while (System.currentTimeMillis() - start < maxTimePerTick);
        }
    }

    @Getter
    private class Playback {
        private final Cursor<Record> cursor;
        private final ICommandSender sender;
        private final PlaybackType playbackType;
        private int count = 0;
        private long lastMessageTime = System.currentTimeMillis();

        private Playback(Cursor<Record> cursor, ICommandSender sender, PlaybackType playbackType) {
            this.cursor = cursor;
            this.sender = sender;
            this.playbackType = playbackType;
        }

        public void onApply() {
            count++;

            long now = System.currentTimeMillis();
            if (now - lastMessageTime > config.get().playback.updateInterval) {
                lastMessageTime = now;
                sender.addChatMessage(Messages.subtle(tr("logger.playback.status", count)));
            }
        }
    }

}
