package com.skcraft.plume.module.commune;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Presence extends Message {

    private String name;
    private Status status;

    @Override
    public void execute(Commune commune) {
    }

    public enum Status {
        ONLINE,
        OFFLINE
    }

}
