package com.skcraft.plume;

import com.sk89q.worldedit.util.eventbus.EventBus;
import com.skcraft.plume.common.auth.Hive;
import com.skcraft.plume.common.ban.BanManager;
import com.skcraft.plume.common.party.PartyManager;
import com.skcraft.plume.common.sql.DatabaseBans;
import com.skcraft.plume.common.sql.DatabaseHive;
import com.skcraft.plume.common.sql.DatabaseManager;
import com.skcraft.plume.common.sql.DatabaseParties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import lombok.Getter;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;

@Mod(modid = Plume.MODID, name = "Plume", dependencies = "required-after:worldedit")
public class Plume {
    
    public static final String MODID = "plume";

    @Instance(MODID)
    public static Plume INSTANCE;
    @SidedProxy(serverSide = "com.skcraft.plume.CommonProxy", clientSide = "com.skcraft.plume.ClientProxy")
    public static CommonProxy PROXY;

    private final EventBus eventBus = new EventBus();
    private Logger logger;
    @Getter private DatabaseManager databaseManager;
    @Getter private BanManager banManager;
    @Getter private PartyManager partyManager;
    private Hive hive;

    public EventBus getEventBus() {
        return eventBus;
    }

    public Logger getLogger() {
        return logger;
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();

        // TODO: Read from a config
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/");
        config.setUsername("plume_dev");
        config.setPassword("plume_dev");
        DataSource dataSource = new HikariDataSource(config);;

        databaseManager = new DatabaseManager(dataSource);
        databaseManager.setDataSchema("plume_data");
        databaseManager.setLogSchema("plume_log");

        banManager = new DatabaseBans(databaseManager);
        hive = new DatabaseHive(databaseManager);
        partyManager = new DatabaseParties(databaseManager);

        PROXY.preInit(event);
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        PROXY.serverStarting(event);
    }

}
