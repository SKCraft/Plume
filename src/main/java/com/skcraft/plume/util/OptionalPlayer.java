package com.skcraft.plume.util;

import lombok.Data;
import net.minecraft.entity.player.EntityPlayerMP;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

@Data
public final class OptionalPlayer {

    private final String name;
    @Nullable
    private final EntityPlayerMP playerEntity;

    public OptionalPlayer(String name, @Nullable EntityPlayerMP playerEntity) {
        checkNotNull(name, "name");
        this.name = name;
        this.playerEntity = playerEntity;
    }

}
