package com.skcraft.plume.util;

import com.skcraft.plume.common.util.WorldVector3i;
import lombok.Data;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ReportedException;
import net.minecraft.world.World;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

@Data
public class BlockSnapshot {

    private static final String DEFAULT_BLOCK_ID = "minecraft:air";

    @Getter private final Block block;
    @Getter private final int meta;
    @Nullable
    @Getter
    private final NBTTagCompound data;

    public BlockSnapshot(Block block, int meta, NBTTagCompound data) {
        checkNotNull(block, "block");
        checkNotNull(meta, "metadata");
        this.block = block;
        this.meta = meta;
        this.data = data;
    }

    public void writeToTag(NBTTagCompound target) {
        String blockName = GameRegistryUtils.getBlockId(block);
        if (blockName != null) {
            target.setString("id", blockName);
            target.setShort("meta", (short) meta);
            if (data != null) {
                NBTTagCompound newData = new NBTTagCompound();
                NBTUtils.copy(data, newData);
                // We can add back this later
                newData.removeTag("x");
                newData.removeTag("y");
                newData.removeTag("z");
                target.setTag("data", newData);
            }
        } else {
            target.setString("id", DEFAULT_BLOCK_ID);
        }
    }

    public void placeInWorld(WorldVector3i location, boolean applyPhysics) {
        try {
            World world = Worlds.getWorldFromId(location.getWorldName());
            boolean wasRestoringBlockSnapshots = world.restoringBlockSnapshots;
            world.restoringBlockSnapshots = true;
            try {
                int x = location.getX();
                int y = location.getY();
                int z = location.getZ();
                world.setBlock(x, y, z, getBlock(), getMeta(), applyPhysics ? 3 : 2);
                world.setBlockMetadataWithNotify(x, y, z, getMeta(), applyPhysics ? 3 : 2);
                world.markBlockForUpdate(x, y, z);
                if (getData() != null) {
                    TileEntity tileEntity = world.getTileEntity(x, y, z);
                    if (tileEntity != null) {
                        NBTTagCompound copy = (NBTTagCompound) getData().copy();
                        copy.setInteger("x", location.getX());
                        copy.setInteger("y", location.getY());
                        copy.setInteger("z", location.getZ());
                        tileEntity.readFromNBT(copy);
                    }
                }
            } finally {
                world.restoringBlockSnapshots = wasRestoringBlockSnapshots;
            }
        } catch (NoSuchWorldException ignored) {
        }
    }

    public static BlockSnapshot readFromTag(NBTTagCompound tag) {
        String id = tag.getString("id");
        short metadata = tag.getShort("meta");
        NBTTagCompound data = null;
        try {
            data = tag.getCompoundTag("data");
        } catch (ReportedException ignored) {
        }

        Block block = GameRegistryUtils.fromBlockId(id);
        if (block == GameRegistryUtils.getDefaultBlock()) {
            metadata = 0;
        }

        return new BlockSnapshot(block, metadata, data);
    }

    public static BlockSnapshot toSnapshot(BlockState state) {
        NBTTagCompound tag = new NBTTagCompound();
        state.writeDataToTag(tag);
        if (tag.hasNoTags()) {
            tag = null;
        }
        return new BlockSnapshot(state.getBlock(), state.getMeta(), tag);
    }

    public static BlockSnapshot toSnapshot(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        NBTTagCompound data = null;
        if (tileEntity != null) {
            data = new NBTTagCompound();
            tileEntity.writeToNBT(data);
        }
        return new BlockSnapshot(block, meta, data);
    }

    public static BlockSnapshot toSnapshot(net.minecraftforge.common.util.BlockSnapshot snapshot) {
        NBTTagCompound temp = new NBTTagCompound();
        snapshot.writeToNBT(temp);
        NBTTagCompound data = temp.getBoolean("hasTE") ? temp.getCompoundTag("tileEntity") : null;
        return new BlockSnapshot(snapshot.getReplacedBlock(), snapshot.meta, data);
    }

}
