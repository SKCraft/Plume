package com.skcraft.plume.module.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.skcraft.plume.common.event.lifecycle.InitializationEvent;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Server;

@Module(name = "metrics-server", desc = "Register metrics related to the server", hidden = true)
public class ServerMetrics {

    @Inject private MetricRegistry metricRegistry;

    @Subscribe
    public void onInitialization(InitializationEvent event) {
        metricRegistry.register(MetricRegistry.name("playerCount"), (Gauge<Integer>) () -> Server.getOnlinePlayers().size());
    }

}
