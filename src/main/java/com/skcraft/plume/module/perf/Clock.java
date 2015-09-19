package com.skcraft.plume.module.perf;

import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.service.ClockHistory;
import com.skcraft.plume.util.Messages;
import net.minecraft.command.ICommandSender;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "clock", desc = "Adds a command to see server tick rate")
public class Clock {

    @Inject private ClockHistory clockHistory;

    @Command(aliases = "clock", desc = "Show the tick rate of the server")
    @Require("plume.clock")
    public void clock(@Sender ICommandSender sender) {
        Double[] averages = clockHistory.getAverageTickTimes(5, 30, 300);

        sender.addChatMessage(Messages.info(tr("clock.tickTimes",
                averages[0] != null ? tr("clock.time", averages[0]) : tr("clock.unknown"),
                averages[1] != null ? tr("clock.time", averages[1]) : tr("clock.unknown"),
                averages[2] != null ? tr("clock.time", averages[2]) : tr("clock.unknown"))));
        sender.addChatMessage(Messages.info(tr("clock.tickRate",
                averages[0] != null ? tr("clock.rate", ClockHistory.toTickRate(averages[0])) : tr("clock.unknown"),
                averages[1] != null ? tr("clock.rate", ClockHistory.toTickRate(averages[1])) : tr("clock.unknown"),
                averages[2] != null ? tr("clock.rate", ClockHistory.toTickRate(averages[2])) : tr("clock.unknown"))));
    }

}
