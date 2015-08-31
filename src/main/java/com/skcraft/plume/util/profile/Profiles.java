package com.skcraft.plume.util.profile;

import com.mojang.authlib.GameProfile;
import com.skcraft.plume.common.UserId;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Profiles {

    private Profiles() {
    }

    public static UserId fromProfile(GameProfile gameProfile) {
        checkNotNull(gameProfile, "gameProfile");
        return new UserId(gameProfile.getId(), gameProfile.getName());
    }

    public static UserId fromPlayer(EntityPlayer player) {
        return fromProfile(player.getGameProfile());
    }

    public static UserId fromCommandSender(ICommandSender sender) {
        if (sender instanceof EntityPlayer) {
            return fromPlayer((EntityPlayer) sender);
        } else if (sender instanceof MinecraftServer) {
            return new UserId(UUID.fromString("00000000-0000-0000-0000-000000000000"), "*CONSOLE*");
        } else {
            return new UserId(UUID.nameUUIDFromBytes(sender.getCommandSenderName().getBytes()), sender.getCommandSenderName());
        }
    }

}
