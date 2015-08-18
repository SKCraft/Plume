package com.skcraft.plume.common.sql;

import com.skcraft.plume.common.UserId;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.dbunit.DatabaseUnitException;
import org.dbunit.IDatabaseTester;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.CompositeOperation;
import org.dbunit.operation.DatabaseOperation;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class MockDatabase {

    public static final UserId SK_USER = new UserId(UUID.fromString("0ea8eca3-dbf6-47cc-9d1a-c64551ca975c"), "sk89q");
    public static final UserId VINCENT_USER = new UserId(UUID.fromString("a31a3813-4b01-4022-ba46-29415975c4c5"), "vincent");
    public static final String MOCK_SERVER = "creative";

    private static final Object instanceLock = new Object();
    private static MockDatabase instance;

    private static final String DRIVER = "com.mysql.jdbc.Driver";
    private static final String BASE_URL =  "jdbc:mysql://localhost:3306/";
    private static final String USERNAME = "plume_dev";
    private static final String PASSWORD = "plume_dev";
    private static final String DATA_SCHEMA = "plume_data";
    private static final String LOG_SCHEMA = "plume_log";

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final DatabaseOperation SETUP_OPERATION = new CompositeOperation(new DatabaseOperation[] {
            new SetupConnection(),
            DatabaseOperation.TRUNCATE_TABLE,
            new ResetConnection(),
            DatabaseOperation.INSERT});

    private final DataSource dataSource;

    private MockDatabase() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(BASE_URL);
        config.setUsername(USERNAME);
        config.setPassword(PASSWORD);
        this.dataSource = new HikariDataSource(config);
    }

    public void loadData() {
        importData("test_data.xml", DATA_SCHEMA);
        importData("test_log.xml", LOG_SCHEMA);
    }

    private void importData(String filename, String schema) {
        try {
            IDataSet dataSet = new FlatXmlDataSetBuilder().build(MockDatabase.class.getResource(filename));
            IDatabaseTester databaseTester = new JdbcDatabaseTester(DRIVER, BASE_URL + schema, USERNAME, PASSWORD);
            databaseTester.setSetUpOperation(SETUP_OPERATION);
            databaseTester.setDataSet(dataSet);
            databaseTester.onSetup();
            databaseTester.getConnection().close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup database for test", e);
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public static MockDatabase getInstance() {
        synchronized (instanceLock) {
            if (instance == null) {
                instance = new MockDatabase();
            }
            return instance;
        }
    }

    public DatabaseManager createDatabaseManager() {
        DatabaseManager manager = new DatabaseManager(getDataSource());
        manager.setDataSchema(DATA_SCHEMA);
        manager.setLogSchema(LOG_SCHEMA);
        return manager;
    }

    public static Date parseDate(String text) {
        try {
            return simpleDateFormat.parse(text);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private static class SetupConnection extends DatabaseOperation {
        @Override
        public void execute(IDatabaseConnection connection, IDataSet dataSet) throws DatabaseUnitException, SQLException {
            connection.getConfig().setProperty(DatabaseConfig.PROPERTY_ESCAPE_PATTERN, "`?`");
            Statement stmt = connection.getConnection().createStatement();
            stmt.executeQuery("SET FOREIGN_KEY_CHECKS = 0;");
        }
    }

    private static class ResetConnection extends DatabaseOperation {
        @Override
        public void execute(IDatabaseConnection connection, IDataSet dataSet) throws DatabaseUnitException, SQLException {
            Statement stmt = connection.getConnection().createStatement();
            stmt.executeQuery("SET FOREIGN_KEY_CHECKS = 1;");
        }
    }

}
