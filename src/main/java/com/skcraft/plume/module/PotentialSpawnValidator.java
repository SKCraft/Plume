package com.skcraft.plume.module;

import com.skcraft.plume.common.util.module.Module;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import lombok.extern.java.Log;
import net.minecraft.world.biome.BiomeGenBase.SpawnListEntry;
import net.minecraftforge.event.world.WorldEvent;

import java.util.Iterator;

@Module(name = "potential-spawn-validator", desc = "Block spawns with invalid weights")
@Log
public class PotentialSpawnValidator {

    @SubscribeEvent
    public void onPotentialSpawns(WorldEvent.PotentialSpawns event) {
        Iterator<SpawnListEntry> it = event.list.iterator();
        while (it.hasNext()) {
            SpawnListEntry entry = it.next();
            if (entry.itemWeight <= 0) {
                log.warning("itemWeight = " + entry.itemWeight + " for " + entry.entityClass + " -> removed from list");
                it.remove();
            }
        }
    }

}
