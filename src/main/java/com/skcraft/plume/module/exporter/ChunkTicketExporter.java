package com.skcraft.plume.module.exporter;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.skcraft.plume.util.ChunkManagerUtils;
import com.skcraft.plume.util.Worlds;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChunkTicketExporter implements CSVExporter {

    private final List<String[]> data = Lists.newArrayList();

    @Override
    public void collectData() throws NoSuchFieldException, IllegalAccessException {
        Map<World, Multimap<String, Ticket>> tickets = ChunkManagerUtils.getTickets();
        for (Map.Entry<World, Multimap<String, Ticket>> entry : tickets.entrySet()) {
            for (Ticket ticket : entry.getValue().values()) {
                for (ChunkCoordIntPair chunkPos : ticket.getChunkList()) {
                    data.add(new String[] {
                            String.valueOf(Objects.hashCode(ticket)),
                            Worlds.getWorldId(entry.getKey()),
                            String.valueOf(chunkPos.chunkXPos),
                            String.valueOf(chunkPos.chunkZPos),
                            ticket.getModId(),
                            ticket.getPlayerName(),
                            ticket.getModData().toString()
                    });
                }
            }
        }
    }

    @Override
    public void writeData(CSVWriter writer) {
        writer.writeNext(new String[] {
                "ID",
                "World",
                "X",
                "Z",
                "Mod",
                "Player",
                "Data"
        });
        writer.writeAll(data);
    }

}
