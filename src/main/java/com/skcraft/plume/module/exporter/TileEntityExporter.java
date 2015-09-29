package com.skcraft.plume.module.exporter;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Lists;
import com.skcraft.plume.util.GameRegistryUtils;
import com.skcraft.plume.util.Worlds;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

import java.util.List;

public class TileEntityExporter implements CSVExporter {

    private final List<TileEntity> tileEntities = Lists.newArrayList();

    @Override
    public void collectData() {
        for (WorldServer world : MinecraftServer.getServer().worldServers) {
            for (Object entity : world.loadedTileEntityList) {
                tileEntities.add((TileEntity) entity);
            }
        }
    }

    @Override
    public void writeData(CSVWriter writer) {
        writer.writeNext(new String[] { "World", "X", "Y", "Z", "Class", "Block" });

        for (TileEntity tileEntity : tileEntities) {
            writer.writeNext(new String[] {
                    Worlds.getWorldId(tileEntity.getWorldObj()),
                    String.valueOf(tileEntity.xCoord),
                    String.valueOf(tileEntity.yCoord),
                    String.valueOf(tileEntity.zCoord),
                    tileEntity.getClass().getName(),
                    GameRegistryUtils.getBlockId(tileEntity.blockType)
            });
        }
    }

}
