package com.skcraft.plume.module.exporter;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Lists;
import com.skcraft.plume.util.Worlds;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import java.util.List;

public class ChunkExporter implements CSVExporter {

    private final List<String[]> data = Lists.newArrayList();

    @Override
    public void collectData() {
        for (WorldServer world : MinecraftServer.getServer().worldServers) {
            for (Object object : world.theChunkProviderServer.loadedChunks) {
                Chunk chunk = (Chunk) object;

                int entityCount = 0;
                for (List list : chunk.entityLists) {
                    entityCount += list.size();
                }

                data.add(new String[] {
                        Worlds.getWorldId(world),
                        String.valueOf(chunk.xPosition),
                        String.valueOf(chunk.zPosition),
                        String.valueOf(entityCount),
                        chunk.isTerrainPopulated ? "Yes" : "No",
                        chunk.isLightPopulated ? "Yes" : "No",
                        chunk.hasEntities ? "Yes" : "No",
                        String.valueOf(chunk.lastSaveTime),
                        String.valueOf(chunk.heightMapMinimum),
                        String.valueOf(chunk.inhabitedTime)

                });
            }
        }
    }

    @Override
    public void writeData(CSVWriter writer) {
        writer.writeNext(new String[] {
                "World",
                "X",
                "Z",
                "Entity Count",
                "Terrain Populated",
                "Light Populated",
                "Has Entities Flag",
                "Last Save Time",
                "Height Map Minimum",
                "Inhabited Time"
        });
        writer.writeAll(data);
    }

}
