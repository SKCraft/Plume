package com.skcraft.plume.common.sql;

import com.skcraft.plume.common.UserId;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
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

public class TestDatabase {

    public static final UserId SK_USER = new UserId(UUID.fromString("0ea8eca3-dbf6-47cc-9d1a-c64551ca975c"), "sk89q");
    public static final UserId VINCENT_USER = new UserId(UUID.fromString("a31a3813-4b01-4022-ba46-29415975c4c5"), "vincent");
    public static final String MOCK_SERVER = "creative";

    private static final String DATA_DATABASE = "plume_data";
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private static final DatabaseOperation SETUP_OPERATION = new CompositeOperation(new DatabaseOperation[] {
            new SetupConnection(),
            DatabaseOperation.TRUNCATE_TABLE,
            new ResetConnection(),
            DatabaseOperation.INSERT});

    private final String driver = "com.mysql.jdbc.Driver";
    private final String url;
    private final String username = "plume_dev";
    private final String password = "plume_dev";
    @Getter private final String database;
    private final DataSource dataSource;

    public TestDatabase(String database) {
        this.url = "jdbc:mysql://localhost:3306/" + database;
        this.database = database;

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        this.dataSource = new HikariDataSource(config);
    }

    protected void setupDatabase() {
        try {
            IDataSet dataSet = new FlatXmlDataSetBuilder().build(TestDatabase.class.getResource("test_data.xml"));
            IDatabaseTester databaseTester = new JdbcDatabaseTester(driver, url, username, password);
            databaseTester.setSetUpOperation(SETUP_OPERATION);
            databaseTester.setDataSet(dataSet);
            databaseTester.onSetup();
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup database for test", e);
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public static Date parseDate(String text) {
        try {
            return simpleDateFormat.parse(text);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static TestDatabase getPrimary() {
        return new TestDatabase(DATA_DATABASE);
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
