package com.skcraft.plume.module.stats;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.event.lifecycle.InitializationEvent;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import net.minecraft.command.ICommandSender;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "uptime", desc = "Show how long the server has been running")
public class Uptime {

    @Inject
    private MetricRegistry metricRegistry;
    private final PeriodFormatter formatter = PeriodFormat.wordBased();

    private static long getUptime() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        return runtimeBean.getUptime();
    }

    @Subscribe
    public void onInitialization(InitializationEvent event) {
        metricRegistry.register("uptime", (Gauge<Long>) () -> getUptime());
    }

    @Command(aliases = "uptime", desc = "Show how long the server has been up")
    @Require("plume.uptime")
    public void uptime(@Sender ICommandSender sender) {
        // TODO: Not fully localized!
        sender.addChatMessage(Messages.info(tr("uptime.uptime", formatter.print(new Period(getUptime())))));
    }

}
