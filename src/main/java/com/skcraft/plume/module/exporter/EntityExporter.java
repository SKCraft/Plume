package com.skcraft.plume.module.exporter;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Lists;
import com.skcraft.plume.util.Worlds;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import java.util.List;

public class EntityExporter implements CSVExporter {

    private final List<Entity> entities = Lists.newArrayList();

    @Override
    public void collectData() {
        for (WorldServer world : MinecraftServer.getServer().worldServers) {
            for (Object entity : world.loadedEntityList) {
                entities.add((Entity) entity);
            }
        }
    }

    @Override
    public void writeData(CSVWriter writer) {
        writer.writeNext(new String[] { "Entity ID", "World", "X", "Y", "Z", "Class", "UUID", "Ticks Existed", "Dead" });

        for (Entity entity : entities) {
            writer.writeNext(new String[] {
                    String.valueOf(entity.getEntityId()),
                    Worlds.getWorldId(entity.worldObj),
                    String.valueOf(entity.posX),
                    String.valueOf(entity.posY),
                    String.valueOf(entity.posZ),
                    entity.getClass().getName(),
                    entity.getUniqueID().toString(),
                    String.valueOf(entity.ticksExisted),
                    entity.isDead ? "Yes" : "No"
            });
        }
    }
}
