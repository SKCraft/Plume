package com.skcraft.plume.module;

import com.mojang.authlib.GameProfile;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import ninja.leaping.configurate.objectmapping.Setting;

@Module(name = "operator-check")
public class OperatorCheck {

    @InjectConfig("operator_check")
    private Config<CheckConfig> config;

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (config.get().deopOnJoin) {
            ServerConfigurationManager configManager = MinecraftServer.getServer().getConfigurationManager();
            GameProfile gameProfile = event.player.getGameProfile();
            if (configManager.func_152596_g(gameProfile)) { /* isOp() */
                // Warning: Blocks thread -- underlying map not thread-safe
                configManager.func_152610_b(gameProfile); /* deop() */
            }
        }
    }

    private static class CheckConfig {
        @Setting(comment = "Set to true to remove op status from users that join")
        public boolean deopOnJoin = false;
    }

}
