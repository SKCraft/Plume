package com.skcraft.plume.module.debug;

import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.Stopwatch;
import com.skcraft.plume.common.util.event.Order;
import com.skcraft.plume.common.util.event.Subscribe;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.event.tick.TileEntityTickEvent;
import com.skcraft.plume.util.Messages;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "server-breaker", desc = "Debugging commands to break the server", enabled = false, hidden = true)
@Log
public class ServerBreaker {

    private long stall = 0;
    private long stallTick = 0;
    private boolean crash = false;

    @Command(aliases = "stallserver", desc = "Stall the server")
    @Require("plume.serverbreaker.stall")
    public void stallServer(@Sender ICommandSender sender, int time) {
        Messages.broadcast(Messages.info(tr("serverBreaker.stallServerCalled", sender.getCommandSenderName())));
        stall = time > 0 ? time : 1;
    }

    @Command(aliases = "stalltileentity", desc = "Stall the next tile entity")
    @Require("plume.serverbreaker.stall")
    public void stallNextTileEntity(@Sender ICommandSender sender, int time) {
        Messages.broadcast(Messages.info(tr("serverBreaker.stallTileEntityCalled", sender.getCommandSenderName())));
        stallTick = time > 0 ? time : 1;
    }

    @Command(aliases = "crashserver", desc = "Crash the server")
    @Require("plume.serverbreaker.crash")
    public void crashServer(@Sender ICommandSender sender) {
        Messages.broadcast(Messages.info(tr("serverBreaker.crashServerCalled", sender.getCommandSenderName())));
        crash = true;
    }

    @Subscribe(order = Order.VERY_LATE)
    public void onTileEntityTick(TileEntityTickEvent event) {
        if (stallTick > 0) {
            long time = stallTick;
            stallTick = 0;

            // Stall in a stopwatch because it's only at this time that we end up in the try {} catch
            event.getStopwatches().add(new Stopwatch() {
                @Override
                public void start() {
                    log.info("Proceeding to stall the server due to /stalltileentity usage...");
                    try {
                        for (int i = 0; i < time; i++) {
                            Thread.sleep(1000);
                        }
                    } catch (InterruptedException e) {
                        log.info("Server stall interrupted!");
                        Thread.interrupted();
                    }
                }

                @Override
                public void stop() {
                }
            });
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
        if (stall > 0) {
            log.info("Proceeding to stall the server due to /stallserver usage...");
            try {
                while (stall > 0) {
                    Thread.sleep(1000);
                    stall--;
                }
            } catch (InterruptedException e) {
                log.info("Server stall interrupted!");
                Thread.interrupted();
            }
        }

        if (crash) {
            log.info("Proceeding to crash the server due to /crashserver usage...");
            while (true) {
                throw new RuntimeException("Server crash requested with /crashserver");
            }
        }
    }

}
