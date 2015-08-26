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

    @Inject private TickExecutorService tickExecutor;
    @InjectConfig("restart_commands") private Config<RestartConfig> config;
    private Timer timer;
    public boolean restarting = false;

    @Command(aliases = "restart", desc = "Restarts the server with a countdown. /restart abort to cancel.")
    @Require("plume.restart")
    public void restart(@Sender ICommandSender sender, String arg) {
        if (arg.equals("abort") || arg.equals("cancel")) {
            if (restarting) {
                timer.cancel();
                timer.purge();
                timer = null;
                restarting = false;
                tickExecutor.execute(() -> {
                    Messages.broadcastInfo(tr("restart.broadcast.canceled"));
                });
            } else {
                tickExecutor.execute(() -> {
                    sender.addChatMessage(Messages.error(tr("restart.cancel.failed")));
                });
            }
        } else {
            try {
                int time = Integer.parseInt(arg);

                if (restarting) {
                    tickExecutor.execute(() -> {
                        sender.addChatMessage(Messages.error(tr("restart.alreadyinprogress")));
                    });
                } else if (time > config.get().maxCountdown || time < 0) {
                    tickExecutor.execute(() -> {
                        sender.addChatMessage(Messages.error(tr("restart.outofrange", config.get().maxCountdown)));
                    });
                } else {
                    restarting = true;
                    timer = new Timer();
                    timer.scheduleAtFixedRate(new RestartTask(time), 1000, 1000);
                    if (time % config.get().period != 0) {
                        tickExecutor.execute(() -> {
                            Messages.broadcastInfo(tr("restart.broadcast.first", time));
                        });
                    }

                    /*
                    for (String name : MinecraftServer.getServer().getAllUsernames()) {
                        if (name != null) {
                            EntityPlayerMP player = Server.findPlayer(name);
                            //player.playSound("random.explode1", 1.0f, 1.0f); //skcraftshenanigans:shutdown
                            player.worldObj.playSoundAtEntity(player, "random.explode", 1.0f, 1.0f);
                        }
                    }
                    */
                }
            } catch (NumberFormatException e) {
                tickExecutor.execute(() -> {
                    sender.addChatMessage(Messages.error(tr("restart.invalidparam", e.getMessage())));
                });
            }
        }
    }

    private class RestartTask extends TimerTask {
        private int timeLeft;

        public RestartTask(int countdownTime) {
            this.timeLeft = countdownTime;
        }

        public void run() {
            if (timeLeft > 0) {
                if (timeLeft % config.get().period == 0 && timeLeft >= 5) {
                    tickExecutor.execute(() -> {
                        Messages.broadcastInfo(tr("restart.broadcast.in", timeLeft));
                    });
                } else if (timeLeft <= 5) {
                    tickExecutor.execute(() -> {
                        Messages.broadcastInfo(tr("restart.broadcast.in", timeLeft));
                    });
                }
                timeLeft--;
            }  else {
                restarting = false;
                timer.cancel();
                timer.purge();
                timer = null;
                tickExecutor.execute(() -> {
                    Messages.broadcastInfo(tr("restart.broadcast.imminent"));
                    Server.shutdown(tr("restart.kickmessage"));
                });
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
