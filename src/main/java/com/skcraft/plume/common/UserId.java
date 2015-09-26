package com.skcraft.plume.common;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

@Data
@EqualsAndHashCode(of = "uuid")
public class UserId implements Serializable {

    private static final long serialVersionUID = -9011911669335290330L;
    private final UUID uuid;
    private String name;

    public UserId(UUID uuid, String name) {
        checkNotNull(uuid, "uuid");
        checkNotNull(name, "name");
        this.uuid = uuid;
        this.name = name;
    }

    public EntityPlayerMP getPlayerOfThis() {
        if (MinecraftServer.getServer().getConfigurationManager().playerEntityList == null)
            return null;

        for (EntityPlayerMP p : (List<EntityPlayerMP>) MinecraftServer.getServer().getConfigurationManager().playerEntityList)
            if (p.getGameProfile().getId().equals(this.getUuid()))
                return p;
        return null;
    }
}
