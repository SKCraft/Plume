package com.skcraft.plume.module.perf.profiler;

import com.google.common.collect.Lists;
import com.google.common.io.CharSource;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
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
import net.minecraft.command.ICommandSender;
import ninja.leaping.configurate.objectmapping.Setting;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "chunk-profiler", desc = "Builds a report of objects that cause implicit chunk loading")
public class ChunkProfiler {

    public static final String PERMISSION = "plume.chunkprofiler";

    private final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
    @Inject private EventBus eventBus;
    @Inject private Broadcaster broadcaster;
    @Inject private ProfilerExecutor<ChunkLoadProfiler> profilerExecutor;
    @Inject private TickExecutorService tickExecutor;
    @InjectConfig("chunk_profiler") private Config<ChunkProfilerConfig> config;

    @Command(aliases = "start", desc = "Start monitoring of chunk loads")
    @Group(@At("chunkprofiler"))
    @Require(PERMISSION)
    public void start(@Sender ICommandSender sender, @Optional Integer delay) {
        try {
            delay = config.get().clampDelay(delay);
            ListenableFuture<ChunkLoadProfiler> future = profilerExecutor.submit(new ChunkLoadProfiler(), delay, TimeUnit.SECONDS);
            broadcaster.broadcast(Messages.info(tr("chunkProfiler.started", delay, sender.getCommandSenderName())), PERMISSION);

            Deferreds.makeDeferred(future)
                    .tap(() -> {
                        broadcaster.broadcast(Messages.info(tr("chunkProfiler.completed")), PERMISSION);
                    }, tickExecutor)
                    .filter(profiler -> {
                        ReportGenerationEvent event = new ReportGenerationEvent("chunk-loads", "txt", CharSource.wrap(generateReport(profiler)));
                        eventBus.post(event);
                        return event.getMessages();
                    })
                    .done(messages -> {
                        for (String message : messages) {
                            broadcaster.broadcast(Messages.info(message), PERMISSION);
                        }
                    });
        } catch (AlreadyProfilingException e) {
            sender.addChatMessage(Messages.error(tr("chunkProfiler.alreadyProfiling")));
        }
    }

    @Command(aliases = "stop", desc = "Stop monitoring of chunk loads")
    @Group(@At("chunkprofiler"))
    @Require(PERMISSION)
    public void stop(@Sender ICommandSender sender) {
        try {
            profilerExecutor.stop();
        } catch (NotProfilingException e) {
            sender.addChatMessage(Messages.error(tr("chunkProfiler.noOngoing")));
        }
    }

    private String generateReport(ChunkLoadProfiler profiler) {
        List<CountableStackTrace> results = Lists.newArrayList(profiler.getCounts().values());
        Collections.sort(results);

        StringBuilder builder = new StringBuilder();
        for (CountableStackTrace countable : results) {
            builder.append("Caused ").append(countable.getCount()).append(" chunk load(s):\r\n");
            for (StackTraceElement element : countable.getStackTrace()) {
                builder.append("\t").append(element.getClassName()).append(".").append(element.getMethodName()).append("()\r\n");
            }
            builder.append("\r\n=============================================\r\n\r\n");
        }

        return builder.toString();
    }

    private static class ChunkProfilerConfig {
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
