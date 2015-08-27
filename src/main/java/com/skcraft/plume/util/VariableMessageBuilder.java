package com.skcraft.plume.util;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import com.skcraft.plume.common.util.StringInterpolation;
import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.regex.Pattern;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Singleton
public class VariableMessageBuilder {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([^\\}]+)\\}");

    public String interpolate(String text, MessageContext context) {
        return StringInterpolation.interpolate(VARIABLE_PATTERN, text, context);
    }

    public static class MessageContext implements Function<String, String> {
        @Getter @Nullable
        private EntityPlayer player;

        public MessageContext setPlayer(@Nullable EntityPlayer player) {
            this.player = player;
            return this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public String apply(String s) {
            switch (s) {
                case "online.count": return String.valueOf(MinecraftServer.getServer().getConfigurationManager().getCurrentPlayerCount());
                case "max": return String.valueOf(MinecraftServer.getServer().getConfigurationManager().getMaxPlayers());
                case "viewDistance": return String.valueOf(MinecraftServer.getServer().getConfigurationManager().getViewDistance());
                case "buildLimit": return String.valueOf(MinecraftServer.getServer().getBuildLimit());
                case "animalsEnabled": return toBoolean(MinecraftServer.getServer().getCanSpawnAnimals());
                case "npcsEnabled": return toBoolean(MinecraftServer.getServer().getCanSpawnNPCs());
                case "hostname": return MinecraftServer.getServer().getHostname();
                case "port": return String.valueOf(MinecraftServer.getServer().getPort());
                case "motd": return MinecraftServer.getServer().getMOTD();
                case "modName": return MinecraftServer.getServer().getServerModName();
                case "serverOwner": return MinecraftServer.getServer().getServerOwner();
                case "spawnProtectionSize": return String.valueOf(MinecraftServer.getServer().getSpawnProtectionSize());
                case "online.names": return Joiner.on(tr("listSeparator") + " ")
                        .join(Lists.transform(
                                (List<EntityPlayer>) MinecraftServer.getServer().getConfigurationManager().playerEntityList,
                                input -> input.getGameProfile().getName()));
                case "online.displayNames": return Joiner.on(tr("listSeparator") + " ")
                        .join(Lists.transform(
                                (List<EntityPlayer>) MinecraftServer.getServer().getConfigurationManager().playerEntityList,
                                input -> input.getDisplayName()));
            }

            if (player != null) {
                switch (s) {
                    case "player.displayName": return player.getDisplayName();
                    case "player.uuid": return player.getGameProfile().getId().toString();
                    case "player.name": return player.getGameProfile().getName();
                    case "player.score": return String.valueOf(player.getScore());
                }
            }

            return null;
        }

        private static String toBoolean(boolean b) {
            return b ? tr("yes") : tr("no");
        }
    }

}
