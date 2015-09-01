package com.skcraft.plume.common.module;

import com.google.inject.Singleton;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.service.sql.DatabaseManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ninja.leaping.configurate.objectmapping.Setting;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

@Singleton
public class MySQLPool {

    @InjectConfig("mysql/pool")
    private Config<DatabaseConfig> config;

    private DatabaseManager database;

    public DatabaseManager getDatabase() {
        synchronized (this) {
            if (database == null) {
                HikariConfig hikariConfig = new HikariConfig();
                hikariConfig.setPoolName("Plume MySQL Services");
                hikariConfig.setMaximumPoolSize(config.get().maxPoolSize);
                hikariConfig.setConnectionTimeout(config.get().connectionTimeout);
                hikariConfig.setValidationTimeout(config.get().validationTimeout);
                hikariConfig.setIdleTimeout(config.get().idleTimeout);
                hikariConfig.setMaxLifetime(config.get().maxLifetime);
                hikariConfig.setJdbcUrl(config.get().url);
                hikariConfig.setUsername(config.get().username);
                hikariConfig.setPassword(config.get().password);
                hikariConfig.addDataSourceProperty("serverTimezone", "UTC");
                hikariConfig.addDataSourceProperty("useLegacyDatetimeCode", "false");
                DataSource dataSource = new HikariDataSource(hikariConfig);

                database = new DatabaseManager(dataSource);
            }
        }

        return database;
    }

    private static class DatabaseConfig {
        @Setting(comment = "Such as jdbc:mysql://localhost:3306/")
        private String url = "jdbc:mysql://localhost:3306/";

        @Setting(comment = "The username to connect to the database with")
        private String username = "plume_dev";

        @Setting(comment = "The password to connect to the database with")
        private String password = "plume_dev";

        @Setting(comment = "The schema for storing data")
        private String schema = "plume_dev";

        @Setting(comment = "Maximum number of active connections in the connection pool")
        private int maxPoolSize = 10;

        @Setting(comment = "Maximum number of milliseconds that the pool will wait for a connection to be established")
        private long connectionTimeout = TimeUnit.SECONDS.toMillis(30);

        @Setting(comment = "Maximum number of milliseconds that the pool will wait for a connection to be validated as alive")
        private long validationTimeout = TimeUnit.SECONDS.toMillis(5);

        @Setting(comment = "Maximum amount of time (in milliseconds) that a connection is allowed to sit idle in the pool")
        private long idleTimeout = TimeUnit.MINUTES.toMillis(10);

        @Setting(comment = "Maximum lifetime of a connection in the pool (once it is no longer in use)")
        private long maxLifetime = TimeUnit.MINUTES.toMillis(30);
    }

}
