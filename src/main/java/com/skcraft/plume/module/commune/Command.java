package com.skcraft.plume.module.commune;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.java.Log;
import net.minecraft.server.MinecraftServer;

import java.util.logging.Level;

@Log
@Data
@EqualsAndHashCode(callSuper = true)
public class Command extends Message {

    private String command;

    @Override
    public void execute(Commune commune) {
        if (commune.getConfig().get().executeCommands) {
            log.log(Level.INFO, "Executing command sent over Commune: " + command);
            MinecraftServer.getServer().getCommandManager().executeCommand(MinecraftServer.getServer(), command);
        } else {
            log.log(Level.INFO, "Cannot execute commands sent over Commune (feature disabled) -- command sent was " + command);
        }
    }

}
