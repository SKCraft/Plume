package com.skcraft.plume.common.module;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.skcraft.plume.common.event.lifecycle.InitializationEvent;
import com.skcraft.plume.common.module.MySQLServices.InjectorModule;
import com.skcraft.plume.common.service.auth.Hive;
import com.skcraft.plume.common.service.ban.BanManager;
import com.skcraft.plume.common.service.claim.ClaimMap;
import com.skcraft.plume.common.service.party.PartyManager;
import com.skcraft.plume.common.service.sql.DatabaseBans;
import com.skcraft.plume.common.service.sql.DatabaseClaims;
import com.skcraft.plume.common.service.sql.DatabaseHive;
import com.skcraft.plume.common.service.sql.DatabaseManager;
import com.skcraft.plume.common.service.sql.DatabaseParties;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import ninja.leaping.configurate.objectmapping.Setting;

import javax.inject.Singleton;

@Module(name = "mysql-services",
        desc = "Provides MySQL-based hive, ban, party, and claim services",
        injectorModule = InjectorModule.class)
public class MySQLServices {

    @InjectConfig("mysql/services")
    private Config<ServicesConfig> config;

    @Inject private MySQLPool pool;
    @Inject private DatabaseHive hive;
    @Inject private DatabaseBans bans;
    @Inject private DatabaseParties parties;
    @Inject private DatabaseClaims claimMap;

    @Subscribe
    public void onInitializationEvent(InitializationEvent event) {
        DatabaseManager database = pool.getDatabase();
        database.setDataSchema(config.get().schema);

        hive.load();
        bans.load();
        parties.load();
        claimMap.load();
    }

    private static class ServicesConfig {
        @Setting(comment = "The schema/database name for the user/bans/etc. tables")
        private String schema = "plume_data";
    }

    public static class InjectorModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(Hive.class).to(DatabaseHive.class).in(Singleton.class);
            bind(BanManager.class).to(DatabaseBans.class).in(Singleton.class);
            bind(PartyManager.class).to(DatabaseParties.class).in(Singleton.class);
            bind(ClaimMap.class).to(DatabaseClaims.class).in(Singleton.class);
        }
    }

}
