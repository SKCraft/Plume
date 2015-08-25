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
import com.skcraft.plume.util.concurrent.BackgroundExecutor;
import com.skcraft.plume.util.concurrent.TickExecutorService;
import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;
import ninja.leaping.configurate.objectmapping.Setting;

import java.util.Timer;
import java.util.TimerTask;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "restart-commands")
@Log
public class RestartCommands {
    @Inject private BackgroundExecutor executor;
    @Inject private TickExecutorService tickExecutorService;
    @InjectConfig("restart_commands") private Config<RestartConfig> config;
    private Timer timer = new Timer("restart");
    public boolean restarting = false;

    @Command(aliases = "restart", desc = "Restarts the server with a countdown. /restart abort to cancel.")
    @Require("plume.restart")
    public void restart(@Sender ICommandSender sender, String arg) {
        if (arg.equals("abort")) {
            if (restarting) {
                timer.cancel();
                restarting = false;
                Messages.broadcastInfo(tr("restart.broadcast.canceled"));
            } else sender.addChatMessage(Messages.error(tr("restart.cancel.failed")));
        } else if (Integer.parseInt(arg) < config.get().maxCountdown && Integer.parseInt(arg) > 0) {
            if (restarting) {
                sender.addChatMessage(Messages.error(tr("restart.alreadyinprogress")));
            } else {
                restarting = true;
                timer.schedule(new RestartTask(Integer.parseInt(arg)), 1000, 1000);
                Messages.broadcastInfo(tr("restart.broadcast.first", Integer.parseInt(arg)));
            }
        } else {
            sender.addChatMessage(Messages.error(tr("restart.invalidparam")));
        }
    }

    private class RestartTask extends TimerTask {
        private int timeLeft;

        public RestartTask(int countdownTime) {
            this.timeLeft = countdownTime;
        }

        public void run() {
            if (timeLeft > 0) {
                if (timeLeft % config.get().period == 0) {
                    Messages.broadcastInfo(tr("restart.broadcast.in", timeLeft));
                }
                timeLeft--;
            }  else {
                restarting = false;
                timer.cancel();
                Messages.broadcastInfo(tr("restart.broadcast.imminent"));
                Server.shutdown(tr("restart.kickmessage"));
            }

        }
    }

    private static class RestartConfig {
        @Setting(comment = "The max time that can be used for a shutdown/restart countdown")
        private int maxCountdown = 240;

        @Setting(comment = "The rate at which shutdown/restart messages are shown")
        private int period = 1;
    }
}
