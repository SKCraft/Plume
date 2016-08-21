package com.skcraft.plume.util;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraftforge.common.util.BlockSnapshot;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class BlockState {

    /**
     * Get the block.
     *
     * @return The block
     */
    public abstract Block getBlock();

    /**
     * Get the metadata.
     *
     * @return The metadata
     */
    public abstract int getMeta();

    /**
     * Write the state's NBT data to the given tag.
     *
     * @param tag The tag
     */
    public abstract void writeDataToTag(NBTTagCompound tag);

    @Override
    public String toString() {
        return getBlock().getUnlocalizedName();
    }

    public static BlockState create(IBlockState state) {
        return new SimpleState(state.getBlock(), state.getBlock().getMetaFromState(state), null);
    }

    public static BlockState create(Block block) {
        return new SimpleState(block, 0, null);
    }

    public static BlockState create(Block block, int meta) {
        return new SimpleState(block, meta, null);
    }

    public static BlockState create(Block block, int meta, @Nullable NBTTagCompound nbt) {
        return new SimpleState(block, meta, nbt);
    }

    public static BlockState wrap(BlockSnapshot snapshot) {
        return new SnapshotState(snapshot);
    }

    public static BlockState getBlockAndMeta(Location3i location) {
        BlockPos pos = new BlockPos(location.getX(), location.getY(), location.getZ());
        IBlockState state = location.getWorld().getBlockState(pos);
        Block block = state.getBlock();
        int meta = state.getBlock().getMetaFromState(state);
        return create(block, meta);
    }

    private static class SimpleState extends BlockState {
        private final Block block;
        private final int meta;
        @Nullable
        private final NBTTagCompound data;

        public SimpleState(Block block, int meta, @Nullable NBTTagCompound data) {
            checkNotNull(block, "block");
            this.block = block;
            this.meta = meta;
            this.data = data;
        }

        @Override
        public Block getBlock() {
            return block;
        }

        @Override
        public int getMeta() {
            return meta;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void writeDataToTag(NBTTagCompound tag) {
            checkNotNull(tag, "tag");
            if (data != null) {
                NBTUtils.copy(data, tag);
            }
        }
    }

    private static class SnapshotState extends BlockState {
        private final BlockSnapshot snapshot;

        public SnapshotState(BlockSnapshot snapshot) {
            checkNotNull(snapshot, "snapshot");
            this.snapshot = snapshot;
        }

        @Override
        public Block getBlock() {
            return snapshot.getReplacedBlock().getBlock();
        }

        @Override
        public int getMeta() {
            return snapshot.meta;
        }

        @Override
        public void writeDataToTag(NBTTagCompound tag) {
            checkNotNull(tag, "tag");
            snapshot.writeToNBT(tag);
        }
    }

}
