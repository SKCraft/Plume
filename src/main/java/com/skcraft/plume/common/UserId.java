package com.skcraft.plume.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.Nullable;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

@Data
@EqualsAndHashCode(of = "uuid")
public class UserId {

    private final UUID uuid;
    @Nullable private String name;

    public UserId(UUID uuid) {
        this(uuid, null);
    }

    public UserId(UUID uuid, String name) {
        checkNotNull(uuid, "uuid");
        this.uuid = uuid;
        this.name = name;
    }

}
