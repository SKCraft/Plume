package com.skcraft.plume.module.exporter;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.skcraft.plume.common.util.event.EventBus;
import com.skcraft.plume.event.report.DecorateReportEvent;
import com.skcraft.plume.event.report.Decorator;
import com.skcraft.plume.event.report.Row;
import com.skcraft.plume.util.Worlds;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import java.util.List;
import java.util.Set;

public class ChunkExporter implements CSVExporter {

    private final EventBus eventBus;
    private final List<ChunkRow> data = Lists.newArrayList();

    @Inject
    public ChunkExporter(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void collectData() {
        for (WorldServer world : MinecraftServer.getServer().worldServers) {
            for (Object object : world.theChunkProviderServer.loadedChunks) {
                Chunk chunk = (Chunk) object;

                int entityCount = 0;
                for (Set<Entity> list : chunk.getEntityLists()) {
                    entityCount += list.size();
                }

                data.add(new ChunkRow(Worlds.getWorldId(world), chunk.xPosition, chunk.zPosition, new String[] {
                        Worlds.getWorldId(world),
                        String.valueOf(chunk.xPosition),
                        String.valueOf(chunk.zPosition),
                        String.valueOf(entityCount),
                        chunk.isTerrainPopulated() ? "Yes" : "No",
                        chunk.isLightPopulated() ? "Yes" : "No",
                        String.valueOf(chunk.getLowestHeight()),
                        String.valueOf(chunk.getInhabitedTime())

                }));
            }
        }
    }

    @Override
    public void writeData(CSVWriter writer) {
        DecorateReportEvent event = new DecorateReportEvent(data);
        eventBus.post(event);

        List<String> columns = Lists.newArrayList(
                "World",
                "X",
                "Z",
                "Entity Count",
                "Terrain Populated",
                "Light Populated",
                "Height Map Minimum",
                "Inhabited Time");

        for (Decorator decorator : event.getDecorators()) {
            columns.addAll(decorator.getColumns());
        }

        writer.writeNext(columns.toArray(new String[columns.size()]));

        for (ChunkRow row : data) {
            List<String> values = Lists.newArrayList(row.values);
            for (Decorator decorator : event.getDecorators()) {
                values.addAll(decorator.getValues(row));
                writer.writeNext(values.toArray(new String[values.size()]));
            }
        }
    }

    private static class ChunkRow implements Row {
        private final String world;
        private final int x;
        private final int z;
        private final String[] values;

        private ChunkRow(String world, int x, int z, String[] values) {
            this.world = world;
            this.x = x;
            this.z = z;
            this.values = values;
        }

        @Override
        public String getWorld() {
            return world;
        }

        @Override
        public int getChunkX() {
            return x;
        }

        @Override
        public int getChunkZ() {
            return z;
        }
    }

}
