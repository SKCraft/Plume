package com.skcraft.plume.common.module;

import com.google.inject.Inject;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.skcraft.plume.common.event.lifecycle.InitializationEvent;
import com.skcraft.plume.common.service.journal.Journal;
import com.skcraft.plume.common.service.journal.JournalBuffer;
import com.skcraft.plume.common.service.sql.DatabaseJournal;
import com.skcraft.plume.common.service.sql.DatabaseManager;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.DataDir;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.common.util.service.ServiceLocator;
import ninja.leaping.configurate.objectmapping.Setting;

@Module(name = "mysql-journal", desc = "Provides MySQL-based journal services")
public class MySQLJournal {

    @Inject private ServiceLocator services;
    @Inject private EventBus eventBus;
    @Inject private MySQLPool pool;
    @InjectConfig("mysql/journal") private Config<JournalConfig> config;

    private DatabaseJournal journal;

    @Subscribe
    public void onInitializationEvent(InitializationEvent event) {
        DatabaseManager database = pool.getDatabase();
        database.setLogSchema(config.get().schema);

        (journal = new DatabaseJournal(database)).load();

        services.register(Journal.class, journal);
    }

    private static class JournalConfig {
        @Setting(comment = "The schema/database name for the block logger tables")
        private String schema = "plume_log";
    }

}
