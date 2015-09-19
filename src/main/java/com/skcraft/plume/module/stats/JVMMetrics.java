package com.skcraft.plume.module.stats;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.common.base.CaseFormat;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.skcraft.plume.common.event.lifecycle.InitializationEvent;
import com.skcraft.plume.common.util.module.Module;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.Map;

@Module(name = "metrics-jvm", desc = "Record JVM metrics")
public class JVMMetrics {

    @Inject private MetricRegistry metricRegistry;

    @Subscribe
    public void onInitialization(InitializationEvent event) {
        metricRegistry.registerAll(NameNormalizer.transform(new MemoryUsageGaugeSet(ManagementFactory.getMemoryMXBean(), Collections.emptyList()), "jvm.memory"));
        metricRegistry.registerAll(NameNormalizer.transform(new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()), "jvm.bufferPool"));
        metricRegistry.registerAll(NameNormalizer.transform(new ClassLoadingGaugeSet(ManagementFactory.getClassLoadingMXBean()), "jvm.classLoading"));
        metricRegistry.register("jvm.fileDescriptors.ratio", new FileDescriptorRatioGauge());
        metricRegistry.registerAll(NameNormalizer.transform(new ThreadStatesGaugeSet(), "jvm.threads"));
    }

    private static class NameNormalizer implements MetricSet {
        private final MetricSet delegate;
        private final String prefix;

        private NameNormalizer(MetricSet delegate, String prefix) {
            this.delegate = delegate;
            this.prefix = prefix;
        }

        @Override
        public Map<String, Metric> getMetrics() {
            Map<String, Metric> transformed = Maps.newHashMap();
            for (Map.Entry<String, Metric> entry : delegate.getMetrics().entrySet()) {
                transformed.put(transform(entry.getKey()), entry.getValue());
            }
            return transformed;
        }

        private String transform(String key) {
            return (prefix.isEmpty() ? "" : prefix + ".") + CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key);
        }

        public static NameNormalizer transform(MetricSet delegate, String prefix) {
            return new NameNormalizer(delegate, prefix);
        }
    }

}
