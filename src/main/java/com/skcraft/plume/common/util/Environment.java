package com.skcraft.plume.common.util;

import com.skcraft.plume.common.service.auth.Context;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.AutoRegister;
import ninja.leaping.configurate.objectmapping.Setting;

@AutoRegister
public class Environment {

    @InjectConfig("environment")
    private Config<EnvironmentConfig> config;

    public String getServerId() {
        return config.get().serverId;
    }

    public Context.Builder update(Context.Builder builder) {
        return builder.put("server", getServerId());
    }

    private static class EnvironmentConfig {
        @Setting(comment = "The server ID of this server")
        private String serverId = "default";
    }

}
