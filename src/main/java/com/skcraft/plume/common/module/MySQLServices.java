package com.skcraft.plume.common.module;

import com.google.inject.Inject;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.auth.*;
import com.skcraft.plume.common.service.ban.BanManager;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.event.lifecycle.InitializationEvent;
import com.skcraft.plume.common.util.service.ServiceLocator;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.common.service.party.PartyCache;
import com.skcraft.plume.common.service.party.PartyManager;
import com.skcraft.plume.common.service.sql.DatabaseBans;
import com.skcraft.plume.common.service.sql.DatabaseHive;
import com.skcraft.plume.common.service.sql.DatabaseManager;
import com.skcraft.plume.common.service.sql.DatabaseParties;
import lombok.Getter;
import ninja.leaping.configurate.objectmapping.Setting;

@Module(name = "mysql-services")
public class MySQLServices {

    @InjectConfig("mysql/services")
    private Config<ServicesConfig> config;

    private final MySQLPool pool;
    private final ServiceLocator services;

    @Getter private Hive hive;
    @Getter private BanManager bans;
    @Getter private PartyManager parties;
    @Getter private UserCache userCache;
    @Getter private PartyCache partyCache;

    @Inject
    public MySQLServices(MySQLPool pool, ServiceLocator services) {
        this.pool = pool;
        this.services = services;
    }

    @Subscribe
    public void onInitializationEvent(InitializationEvent event) {
        DatabaseManager database = pool.getDatabase();
        database.setDataSchema(config.get().schema);

        (hive = new DatabaseHive(database)).load();
        (bans = new DatabaseBans(database)).load();
        (parties = new DatabaseParties(database)).load();

        services.register(Hive.class, hive);
        services.register(BanManager.class, bans);
        services.register(PartyManager.class, parties);

        // Not the ideal way to do it but we'll have to manage for now
        services.register(UserCache.class, userCache = new UserCache(hive));
        services.register(PartyCache.class, partyCache = new PartyCache(parties));

        services.register(Authorizer.class, new HiveAuthorizer());
    }

    private class HiveAuthorizer implements Authorizer {
        @Override
        public Subject getSubject(UserId userId) {
            User user = userCache.getUserIfPresent(userId);
            if (user != null) {
                return user.getSubject();
            } else {
                return NoAccessSubject.INSTANCE;
            }
        }
    }

    private static class ServicesConfig {
        @Setting(comment = "The schema/database name for the user/bans/etc. tables")
        private String schema = "plume_data";
    }

}
