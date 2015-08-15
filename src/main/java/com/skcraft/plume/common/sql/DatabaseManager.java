package com.skcraft.plume.common.sql;

import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.sql.model.data.Data;
import lombok.Getter;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.RenderMapping;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.NameTokenizers;
import org.modelmapper.jooq.RecordValueReader;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static com.google.common.base.Preconditions.checkNotNull;

public class DatabaseManager {

    private final SQLDialect sqlDialect = SQLDialect.MYSQL;
    private final DataSource ds;
    @Getter private final UserIdCache userIdCache;
    @Getter private final ModelMapper modelMapper;
    private final Settings settings;

    public DatabaseManager(DataSource ds, String databaseName) {
        checkNotNull(ds, "ds");
        checkNotNull(databaseName, "databaseName");
        this.ds = ds;
        this.userIdCache = new UserIdCache();
        this.modelMapper = new ModelMapper();
        this.settings = new Settings()
                .withRenderMapping(new RenderMapping()
                        .withSchemata(new MappedSchema()
                                .withInput(Data.DATA.getName())
                                .withOutput(databaseName)));

        modelMapper.getConfiguration().addValueReader(new RecordValueReader());
        modelMapper.getConfiguration().setSourceNameTokenizer(NameTokenizers.UNDERSCORE);
    }

    DSLContext create() throws DataAccessException {
        try {
            Connection conn = ds.getConnection();
            return DSL.using(conn, sqlDialect, settings);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to connect to the database", e);
        }
    }

}
