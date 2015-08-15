package com.skcraft.plume.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(of = "uuid")
public class UserId {

    private UUID uuid;
    private String name;

    public UserId() {
    }

    public UserId(UUID uuid) {
        this.uuid = uuid;
    }

    public UserId(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

}
