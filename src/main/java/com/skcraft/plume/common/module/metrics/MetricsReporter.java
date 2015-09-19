package com.skcraft.plume.common.module.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.google.inject.Inject;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.skcraft.plume.common.event.lifecycle.InitializationEvent;
import com.skcraft.plume.common.util.Environment;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import lombok.extern.java.Log;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

@Module(name = "metrics-reporter", desc = "Make metrics available outside the server", enabled = true)
@Log
public class MetricsReporter {

    @Inject private MetricRegistry metricRegistry;
    @Inject private Environment environment;
    @InjectConfig("metrics_reporter") private Config<ReporterConfig> config;

    @Subscribe
    public void onInitialization(InitializationEvent event) {
        if (config.get().console.enabled) {
            log.info("Registering console metrics printing...");
            ConsoleReporter reporter = ConsoleReporter.forRegistry(metricRegistry)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .build();
            reporter.start(config.get().console.interval, TimeUnit.SECONDS);
        }

        if (config.get().graphite.enabled) {
            log.info("Registering Graphite metrics destination...");
            Graphite graphite = new Graphite(new InetSocketAddress(config.get().graphite.address, config.get().graphite.port));
            GraphiteReporter reporter = GraphiteReporter.forRegistry(metricRegistry)
                    .prefixedWith("plume.servers." + environment.getServerId().replace(".", "_"))
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .filter(MetricFilter.ALL)
                    .build(graphite);
            reporter.start(config.get().graphite.interval, TimeUnit.SECONDS);
        }

        if (config.get().jmx.enabled) {
            log.info("Registering metrics via JMX...");
            JmxReporter reporter = JmxReporter.forRegistry(metricRegistry).build();
            reporter.start();
        }
    }

    private static class ReporterConfig {
        @Setting(comment = "Enable reporting via console")
        private ConsoleConfig console = new ConsoleConfig();

        @Setting(comment = "Enable reporting via JMX")
        private JMXConfig jmx = new JMXConfig();

        @Setting(comment = "Enable reporting to a Graphite instance")
        private GraphiteConfig graphite = new GraphiteConfig();
    }

    @ConfigSerializable
    private static class ConsoleConfig {
        @Setting
        private boolean enabled = false;
        @Setting(comment = "The interval in seconds to report metrics at")
        private int interval = 10;
    }

    @ConfigSerializable
    private static class GraphiteConfig {
        @Setting
        private boolean enabled = false;
        @Setting
        private String address = "127.0.0.1";
        @Setting
        private int port = 22003;
        @Setting(comment = "The interval in seconds to report metrics at")
        private int interval = 10;
    }

    @ConfigSerializable
    private static class JMXConfig {
        @Setting
        private boolean enabled = false;
    }

}
