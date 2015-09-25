package com.skcraft.plume.common.module;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.skcraft.plume.common.event.lifecycle.InitializationEvent;
import com.skcraft.plume.common.module.MySQLJournal.InjectorModule;
import com.skcraft.plume.common.service.journal.Journal;
import com.skcraft.plume.common.service.sql.DatabaseJournal;
import com.skcraft.plume.common.service.sql.DatabaseManager;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import ninja.leaping.configurate.objectmapping.Setting;

import javax.inject.Singleton;

@Module(name = "mysql-journal",
        desc = "Provides MySQL-based journal services",
        injectorModule = InjectorModule.class,
        enabled = false)
public class MySQLJournal {

    @Inject private MySQLPool pool;
    @Inject private Journal journal;
    @InjectConfig("mysql/journal") private Config<JournalConfig> config;

    @Subscribe
    public void onInitializationEvent(InitializationEvent event) {
        DatabaseManager database = pool.getDatabase();
        database.setLogSchema(config.get().schema);

        journal.load();
    }

    private static class JournalConfig {
        @Setting(comment = "The schema/database name for the block logger tables")
        private String schema = "plume_log";
    }

    public static class InjectorModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(Journal.class).to(DatabaseJournal.class).in(Singleton.class);
        }
    }

}
