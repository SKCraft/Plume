package com.skcraft.plume;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraftforge.common.MinecraftForge;

class PlumeForgeModule extends AbstractModule {

    @Override
    protected void configure() {
        bindListener(Matchers.any(), new TypeListener() {
            @Override
            public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
                encounter.register((InjectionListener<I>) MinecraftForge.EVENT_BUS::register);
                encounter.register((InjectionListener<I>) FMLCommonHandler.instance().bus()::register);
            }
        });
    }

}
