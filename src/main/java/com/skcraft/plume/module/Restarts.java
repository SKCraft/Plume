package com.skcraft.plume.module;

import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.Server;
import com.skcraft.plume.util.concurrent.TickExecutorService;
import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import ninja.leaping.configurate.objectmapping.Setting;

import java.util.Timer;
import java.util.TimerTask;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "restarts")
@Log
public class Restarts {

    @Inject private TickExecutorService tickExecutor;
    @InjectConfig("restarts") private Config<RestartConfig> config;
    private Timer timer;
    public boolean restarting = false;

    @Command(aliases = {"restart", "shutdown"}, desc = "Restarts the server with a countdown. /restart abort to cancel.")
    @Require("plume.restart")
    public void restart(@Sender ICommandSender sender, String arg) {
        if (arg.equalsIgnoreCase("abort") || arg.equalsIgnoreCase("cancel")) {
            if (restarting) {
                timer.cancel();
                timer.purge();
                timer = null;
                restarting = false;
                Messages.broadcastInfo(tr("restart.broadcast.canceled"));
            } else {
                sender.addChatMessage(Messages.error(tr("restart.cancel.failed")));
            }
        } else {
            try {
                int time = Integer.parseInt(arg);

                if (restarting) {
                    sender.addChatMessage(Messages.error(tr("restart.alreadyInProgress")));
                } else if (time > config.get().maxCountdown || time < 0) {
                    sender.addChatMessage(Messages.error(tr("restart.outOfRange", config.get().maxCountdown)));
                } else {
                    restarting = true;
                    timer = new Timer();
                    timer.scheduleAtFixedRate(new RestartTask(time), 1000, 1000);

                    Messages.broadcastInfo(tr("restart.broadcast.first", time));

                    for (EntityPlayerMP player : Server.getOnlinePlayers()) {
                        player.worldObj.playSoundAtEntity(player, config.get().shutdownSound, 1.0f, 1.0f);
                    }
                }
            } catch (NumberFormatException e) {
                sender.addChatMessage(Messages.error(tr("restart.invalidParameters", e.getMessage())));
            }
        }
    }

    private class RestartTask extends TimerTask {
        private int timeLeft;
        private int lastMessage;

        public RestartTask(int countdownTime) {
            this.timeLeft = countdownTime;
        }

        public void run() {
            if (timeLeft > 0) {
                int left = timeLeft; // Copy variable because we're going to modify it in a second
                if (timeLeft <= config.get().imminentThreshold) {
                    tickExecutor.execute(() -> {
                        Messages.broadcastInfo(tr("restart.broadcast.imminent", left));
                    });
                } else if (timeLeft <= config.get().urgentThreshold) {
                    tickExecutor.execute(() -> {
                        Messages.broadcastInfo(tr("restart.broadcast.urgent", left));
                    });
                } else if (timeLeft % config.get().subtleInterval == 0) {
                    tickExecutor.execute(() -> {
                        Messages.broadcastInfo(tr("restart.broadcast.in", left));
                    });
                }
                timeLeft--;
            }  else {
                restarting = false;
                timer.cancel();
                timer.purge();
                timer = null;
                tickExecutor.execute(() -> {
                    Server.shutdown(tr("restart.kickMessage"));
                });
            }

        }
    }

    private static class RestartConfig {
        @Setting(comment = "The max time that can be used for a shutdown/restart countdown")
        private int maxCountdown = 240;

        @Setting(comment = "The time interval, in seconds, of the subtle background shutdown messages")
        private int subtleInterval = 5;

        @Setting(comment = "The time threshold, in seconds, when the frequent and obnoxious shutdown warnings are shown")
        private int urgentThreshold = 10;

        @Setting(comment = "The time threshold, in seconds, when the 'IMMINENT SHUTDOWN' messages are shown")
        private int imminentThreshold = 2;

        @Setting(comment = "The sound to play when shutting down")
        public String shutdownSound = "records.stal";
    }
}
