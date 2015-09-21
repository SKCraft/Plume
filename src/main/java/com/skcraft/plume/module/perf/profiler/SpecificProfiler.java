package com.skcraft.plume.module.perf.profiler;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Optional;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.skcraft.plume.command.At;
import com.skcraft.plume.command.Group;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.PathnameBuilder;
import com.skcraft.plume.common.util.concurrent.Deferreds;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.event.tick.EntityTickEvent;
import com.skcraft.plume.event.tick.TileEntityTickEvent;
import com.skcraft.plume.module.perf.profiler.Profiler.ReusableStopwatch;
import com.skcraft.plume.util.Broadcaster;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.Worlds;
import com.skcraft.plume.util.concurrent.TickExecutorService;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import ninja.leaping.configurate.objectmapping.Setting;

import javax.annotation.Nullable;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "specific-profiler", desc = "Provides a tick profiler")
@Log
public class SpecificProfiler {

    public static final String PERMISSION = "plume.specificprofiler";

    @Inject private EventBus eventBus;
    @Inject private Broadcaster broadcaster;
    @Inject private TickExecutorService tickExecutor;
    @Nullable private PendingProfile activeProfile;
    @InjectConfig("specific_profiler") private Config<SpecificProfilerConfig> config;

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent event) {
        PendingProfile profile = this.activeProfile;
        if (profile != null && profile.isCollecting()) {
            ReusableStopwatch stopwatch = profile.getProfiler().getStopwatch();
            Entity entity = event.getEntity();
            stopwatch.set(Worlds.getWorldId(event.getWorld()), entity.getClass().getName(), (int) entity.posX, (int) entity.posY, (int) entity.posZ);
            event.getStopwatches().add(stopwatch);
        }
    }

    @SubscribeEvent
    public void onTileEntityTick(TileEntityTickEvent event) {
        PendingProfile profile = this.activeProfile;
        if (profile != null && profile.isCollecting()) {
            ReusableStopwatch stopwatch = profile.getProfiler().getStopwatch();
            TileEntity tileEntity = event.getTileEntity();
            stopwatch.set(Worlds.getWorldId(event.getWorld()), tileEntity.getClass().getName(), tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
            event.getStopwatches().add(stopwatch);
        }
    }

    @Command(aliases = "start", desc = "Start profiling")
    @Group({@At("sprofiler")})
    @Require(PERMISSION)
    public void start(@Sender ICommandSender sender, @Optional Integer delay) {
        if (activeProfile != null) {
            sender.addChatMessage(Messages.error(tr("specificProfiler.alreadyProfiling")));
        } else {
            if (delay == null) {
                delay = config.get().defaultProfileDuration;
            }
            delay = Math.min(config.get().maxProfileDuration, Math.max(5, delay));
            activeProfile = new PendingProfile(delay * 1000, this::saveProfile);
            broadcaster.broadcast(Messages.info(tr("specificProfiler.profilingStarted", delay, sender.getCommandSenderName())), PERMISSION);
        }
    }

    @Command(aliases = "stop", desc = "Stop profiling")
    @Group({@At("sprofiler")})
    @Require(PERMISSION)
    public void stop(@Sender ICommandSender sender) {
        if (activeProfile == null) {
            sender.addChatMessage(Messages.error(tr("specificProfile.noOngoing")));
        } else {
            activeProfile.stop();
            broadcaster.broadcast(Messages.info(tr("specificProfile.profilingStopped", sender.getCommandSenderName())), PERMISSION);
        }
    }

    private void saveProfile(Profiler profiler) {
        activeProfile = null;

        broadcaster.broadcast(Messages.subtle(tr("specificProfiler.nowSaving")), PERMISSION);

        // See if any other modules provide appenders
        CollectAppendersEvent event = new CollectAppendersEvent(profiler.getTimings());
        eventBus.post(event);

        PathnameBuilder pathBuilder = new PathnameBuilder();
        File file = new File(pathBuilder.interpolate(config.get().csvPath));
        file.getParentFile().getAbsoluteFile().mkdirs();

        ListeningExecutorService tempExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
        try {
            CSVReportBuilder builder = new CSVReportBuilder(profiler.getTimings(), event.getAppenders(), file);
            Deferreds
                    .when(builder, tempExecutor)
                    .done(outputFile -> {
                        broadcaster.broadcast(Messages.info(tr("specificProfiler.saved", outputFile.getAbsolutePath())), PERMISSION);
                    }, tickExecutor)
                    .fail(e -> {
                        broadcaster.broadcast(Messages.error(tr("specificProfiler.saveFailed", file.getAbsolutePath())), PERMISSION);
                        log.log(Level.WARNING, "Failed to save Specific Profile report to " + file.getAbsolutePath(), e);
                    }, tickExecutor);
        } finally {
            // We only use the executor for one task
            tempExecutor.shutdown();
        }
    }

    private static class SpecificProfilerConfig {
        @Setting(comment = "The number of seconds to profile for by default (if not specified)")
        private int defaultProfileDuration = 30;

        @Setting(comment = "The maximum number of seconds to profile for")
        private int maxProfileDuration = 60 * 5;

        @Setting(comment = "The path where the profile results are written")
        private String csvPath = "profiles/specificprofiler-%Y-%m-%d-%h-%i-%s.csv";
    }

}
