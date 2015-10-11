package com.skcraft.plume.util.profiling;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import lombok.extern.java.Log;

import javax.annotation.Nullable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkNotNull;

@Log
public class ProfilerExecutor<T extends Profiler> {

    private final Object lock = new Object();
    private final ScheduledExecutorService scheduledExecutor;
    @Nullable private Run currentRun;

    @Inject
    public ProfilerExecutor(ScheduledExecutorService scheduledExecutor) {
        checkNotNull(scheduledExecutor, "scheduledExecutor");
        this.scheduledExecutor = scheduledExecutor;
    }

    public boolean isRunning() {
        return currentRun != null;
    }

    public ListenableFuture<T> submit(T profiler, long duration, TimeUnit timeUnit) throws AlreadyProfilingException {
        synchronized (lock) {
            if (currentRun == null) {
                profiler.start();

                Future<?> future = scheduledExecutor.schedule(() -> stop(profiler), duration, timeUnit);
                Run run = new Run(profiler, future);
                currentRun = run;
                return run.resultFuture;
            } else {
                throw new AlreadyProfilingException();
            }
        }
    }

    private void stop(T profiler) {
        synchronized (lock) {
            Run run = currentRun;
            if (run != null) {
                if (run.profiler == profiler) {
                    try {
                        run.profiler.stop();
                    } catch (Exception e) {
                        log.log(Level.WARNING, "Failed to stop profiler", e);
                    }
                    run.timerFuture.cancel(false);
                    run.resultFuture.set(run.profiler);
                }
                currentRun = null;
            }
        }
    }

    public void stop() throws NotProfilingException {
        synchronized (lock) {
            Run run = this.currentRun;
            if (run != null) {
                stop(run.profiler);
            } else {
                throw new NotProfilingException();
            }
        }
    }

    private class Run {
        private final T profiler;
        private final Future<?> timerFuture;
        private final SettableFuture<T> resultFuture = SettableFuture.create();

        public Run(T profiler, Future<?> timerFuture) {
            this.profiler = profiler;
            this.timerFuture = timerFuture;
        }
    }

}
