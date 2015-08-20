package com.skcraft.plume.common.config;

import lombok.extern.java.Log;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkNotNull;

@Log
public class HoconConfig<T> implements Config<T> {

    private final ConfigurationLoader<CommentedConfigurationNode> loader;
    private final ObjectMapper<T>.BoundInstance configMapper;
    private ConfigurationNode root;
    private T object;
    private boolean loadAttempted = false;

    public HoconConfig(File file, Class<T> type) throws ConfigLoadException {
        checkNotNull(file, "file");
        loader = HoconConfigurationLoader.builder().setFile(file).build();
        try {
            this.configMapper = ObjectMapper.forClass(type).bindToNew();
            this.root = SimpleCommentedConfigurationNode.root();
            this.object = configMapper.populate(root);
        } catch (ObjectMappingException e) {
            throw new ConfigLoadException(e);
        }
    }

    @Override
    public boolean load() {
        try {
            root = loader.load();
            this.object = configMapper.populate(root);
            return true;
        } catch (ObjectMappingException | IOException e) {
            log.log(Level.WARNING, "Failed to load the configuration file", e);
            return false;
        } finally {
            loadAttempted = true;
        }
    }

    @Override
    public boolean save() {
        if (!loadAttempted) {
            throw new RuntimeException("Call Configuration.load() first");
        }
        try {
            this.configMapper.serialize(root);
            loader.save(root);
            return true;
        } catch (IOException | ObjectMappingException e) {
            log.log(Level.WARNING, "Failed to save the configuration file", e);
            return false;
        }
    }

    public T get() {
        return object;
    }

}
