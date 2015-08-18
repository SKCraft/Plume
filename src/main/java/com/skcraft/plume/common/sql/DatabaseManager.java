package com.skcraft.plume.common.sql;

import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.sql.model.data.Data;
import com.skcraft.plume.common.sql.model.log.Log;
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
    private Settings settings;
    private String dataSchema = "plume_data";
    private String logSchema = "plume_log";

    public DatabaseManager(DataSource ds) {
        checkNotNull(ds, "ds");

        this.ds = ds;
        this.userIdCache = new UserIdCache();
        this.modelMapper = new ModelMapper();
        updateSettings();

        modelMapper.getConfiguration().addValueReader(new RecordValueReader());
        modelMapper.getConfiguration().setSourceNameTokenizer(NameTokenizers.UNDERSCORE);
    }

    public void setDataSchema(String dataSchema) {
        this.dataSchema = dataSchema;
        updateSettings();
    }

    public void setLogSchema(String logSchema) {
        this.logSchema = logSchema;
        updateSettings();
    }

    private void updateSettings() {
        this.settings = new Settings()
                .withRenderMapping(new RenderMapping()
                        .withSchemata(new MappedSchema().withInput(Data.DATA.getName()).withOutput(dataSchema))
                        .withSchemata(new MappedSchema().withInput(Log.LOG.getName()).withOutput(logSchema)));;
    }

    DSLContext create() throws DataAccessException {
        return DSL.using(ds, sqlDialect, settings);
    }

}
