package com.skcraft.plume.module;

import com.mojang.authlib.GameProfile;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import lombok.extern.java.Log;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import ninja.leaping.configurate.objectmapping.Setting;

@Log
@Module(name = "operator-check", desc = "Configurable removal of operator status from all players when they join")
public class OperatorCheck {

    @InjectConfig("operator_check")
    private Config<CheckConfig> config;

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (config.get().deopOnJoin) {
            ServerConfigurationManager configManager = MinecraftServer.getServer().getConfigurationManager();
            GameProfile gameProfile = event.player.getGameProfile();
            if (configManager.getOppedPlayers().func_183026_b(gameProfile)) { /* isOp() */
                log.info("Removing op from " + gameProfile.getName() + " (" + gameProfile.getId() + ")");
                // Warning: Blocks thread -- underlying map not thread-safe
                configManager.getOppedPlayers().removeEntry(gameProfile); /* deop() */
            }
        }
    }

    private static class CheckConfig {
        @Setting(comment = "Set to true to remove op status from users that join")
        public boolean deopOnJoin = false;
    }

}
