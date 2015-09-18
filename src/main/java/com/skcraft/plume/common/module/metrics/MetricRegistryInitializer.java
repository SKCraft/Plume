package com.skcraft.plume.common.module.metrics;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.skcraft.plume.common.module.metrics.MetricRegistryInitializer.InjectorModule;
import com.skcraft.plume.common.util.module.Module;

@Module(name = "metric-registry", hidden = true, injectorModule = InjectorModule.class)
public class MetricRegistryInitializer {

    public static class InjectorModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(MetricRegistry.class).toInstance(new MetricRegistry());
        }
    }

}
