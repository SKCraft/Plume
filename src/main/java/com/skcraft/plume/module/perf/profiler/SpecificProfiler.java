package com.skcraft.plume.module.perf.profiler;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Lists;
import com.google.common.io.CharSource;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Optional;
import com.skcraft.plume.command.At;
import com.skcraft.plume.command.Group;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.event.ReportGenerationEvent;
import com.skcraft.plume.common.util.concurrent.Deferreds;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.event.EventBus;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Broadcaster;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.concurrent.TickExecutorService;
import com.skcraft.plume.util.profiling.AlreadyProfilingException;
import com.skcraft.plume.util.profiling.NotProfilingException;
import com.skcraft.plume.util.profiling.ProfilerExecutor;
import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;
import ninja.leaping.configurate.objectmapping.Setting;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "specific-profiler", desc = "Provides a tick profiler")
@Log
public class SpecificProfiler {

    public static final String PERMISSION = "plume.specificprofiler";

    @Inject private EventBus eventBus;
    @Inject private Broadcaster broadcaster;
    @Inject private ProfilerExecutor<TickProfiler> profilerExecutor;
    @Inject private TickExecutorService tickExecutor;
    @InjectConfig("specific_profiler") private Config<SpecificProfilerConfig> config;

    @Command(aliases = "start", desc = "Start profiling")
    @Group({@At("sprofiler")})
    @Require(PERMISSION)
    public void start(@Sender ICommandSender sender, @Optional Integer delay) {
        try {
            delay = config.get().clampDelay(delay);
            ListenableFuture<TickProfiler> future = profilerExecutor.submit(new TickProfiler(), delay, TimeUnit.SECONDS);
            broadcaster.broadcast(Messages.info(tr("specificProfiler.started", delay, sender.getCommandSenderName())), PERMISSION);

            Deferreds.makeDeferred(future)
                    .tap(() -> {
                        broadcaster.broadcast(Messages.info(tr("specificProfiler.completed")), PERMISSION);
                    }, tickExecutor)
                    .filter(profiler -> {
                        ReportGenerationEvent event = new ReportGenerationEvent("specificprofiler", "csv", CharSource.wrap(generateReport(profiler)));
                        eventBus.post(event);
                        return event.getMessages();
                    })
                    .done(messages -> {
                        for (String message : messages) {
                            broadcaster.broadcast(Messages.info(message), PERMISSION);
                        }
                    });
        } catch (AlreadyProfilingException e) {
            sender.addChatMessage(Messages.error(tr("specificProfiler.alreadyProfiling")));
        }
    }

    @Command(aliases = "stop", desc = "Stop profiling")
    @Group({@At("sprofiler")})
    @Require(PERMISSION)
    public void stop(@Sender ICommandSender sender) {
        try {
            profilerExecutor.stop();
        } catch (NotProfilingException e) {
            sender.addChatMessage(Messages.error(tr("specificProfile.noOngoing")));
        }
    }

    private String generateReport(TickProfiler profiler) throws IOException {
        StringWriter writer = new StringWriter();

        CollectAppendersEvent event = new CollectAppendersEvent(profiler.getTimings());
        eventBus.post(event);

        List<Appender> appenders = Lists.newArrayList(new TimingAppender());
        appenders.addAll(event.getAppenders());

        try (CSVWriter csv = new CSVWriter(writer)) {
            List<String> columns = Lists.newArrayList();
            for (Appender appender : appenders) {
                columns.addAll(appender.getColumns());
            }
            csv.writeNext(columns.toArray(new String[columns.size()]));

            for (Timing timing : profiler.getTimings()) {
                List<String> values = Lists.newArrayList();
                for (Appender appender : appenders) {
                    values.addAll(appender.getValues(timing));
                }
                csv.writeNext(values.toArray(new String[values.size()]));
            }
        }

        return writer.toString();
    }

    private static class SpecificProfilerConfig {
        @Setting(comment = "The number of seconds to profile for by default (if not specified)")
        private int defaultProfileDuration = 30;

        @Setting(comment = "The maximum number of seconds to profile for")
        private int maxProfileDuration = 60 * 5;

        public int clampDelay(Integer delay) {
            if (delay == null) {
                delay = defaultProfileDuration;
            }
            delay = Math.min(maxProfileDuration, Math.max(5, delay));
            return delay;
        }
    }

}
