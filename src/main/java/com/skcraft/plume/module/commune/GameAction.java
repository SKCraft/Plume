package com.skcraft.plume.module.commune;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.java.Log;

@Log
@Data
@EqualsAndHashCode(callSuper = true)
public class GameAction extends Message {

    private String sender;
    private String message;

    @Override
    public void execute(Commune commune) {
    }

}
