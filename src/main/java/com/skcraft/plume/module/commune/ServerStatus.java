package com.skcraft.plume.module.commune;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ServerStatus extends Message {

    private Status status;

    @Override
    public void execute(Commune commune) {
    }

    public enum Status {
        ONLINE,
        OFFLINE
    }

}
