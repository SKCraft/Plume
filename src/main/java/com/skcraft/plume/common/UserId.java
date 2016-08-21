package com.skcraft.plume.common;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.io.Serializable;
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

    @Nullable
    @SuppressWarnings("unchecked")
    public EntityPlayer getEntityPlayer() {
        for (EntityPlayer p : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            if (p.getGameProfile().getId().equals(this.getUuid())) {
                return p;
            }
        }
        return null;
    }
}
