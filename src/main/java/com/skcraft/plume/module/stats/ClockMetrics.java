package com.skcraft.plume.module.stats;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.skcraft.plume.common.event.lifecycle.InitializationEvent;
import com.skcraft.plume.common.util.event.Subscribe;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.service.ClockHistory;

import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

@Module(name = "metrics-clock", desc = "Record server tick rate metrics")
public class ClockMetrics {

    private static final int SNAPSHOT_DURATION = 5;
    private static final long CACHE_TIME = TimeUnit.SECONDS.toNanos(1);

    @Inject private MetricRegistry metricRegistry;
    @Inject private ClockHistory clockHistory;

    private Double[] cachedTickTimes = new Double[1];
    private long cacheTime = 0;

    private synchronized Double[] getAverageTickTimes() {
        long now = System.nanoTime();
        if (now - cacheTime >= CACHE_TIME) {
            cachedTickTimes = clockHistory.getAverageTickTimes(SNAPSHOT_DURATION);
            cacheTime = now;
        }
        return cachedTickTimes;
    }

    @Subscribe
    public void onInitialization(InitializationEvent event) {
        metricRegistry.register(name("tickTime"), (Gauge<Double>) () -> getAverageTickTimes()[0]);
        metricRegistry.register(name("tickRate"), (Gauge<Double>) () -> ClockHistory.toTickRate(getAverageTickTimes()[0]));
    }

}
