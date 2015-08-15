package com.skcraft.plume.common;

import com.google.common.collect.Maps;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;

import javax.sql.DataSource;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkNotNull;

public class DataSourceRegistry {

    private final ConcurrentMap<String, DataSource> dataSourceMap = Maps.newConcurrentMap();

    public void put(String name, DataSource dataSource) {
        checkNotNull(name, "name");
        checkNotNull(dataSource, "dataSource");
        dataSourceMap.put(name, dataSource);
    }

    public DataSource get(String name) throws NoDataSourceException {
        checkNotNull(name, "name");
        DataSource dataSource = dataSourceMap.get(name);
        if (dataSource != null) {
            return dataSource;
        } else {
            throw new NoDataSourceException("No data source by th ename of " + name);
        }
    }

    public void load(CommentedConfigurationNode node) {
        for (Entry<Object, ? extends CommentedConfigurationNode> entry : node.getChildrenMap().entrySet()) {
            String id = String.valueOf(entry.getKey());

            CommentedConfigurationNode entryNode = entry.getValue();

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(entryNode.getNode("url").getString(""));
            config.setUsername(entryNode.getNode("username").getString(""));
            config.setPassword(entryNode.getNode("password").getString(""));

            HikariDataSource ds = new HikariDataSource(config);

            put(id, ds);
        }
    }
}
