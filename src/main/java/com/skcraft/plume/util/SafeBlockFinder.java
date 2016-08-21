package com.skcraft.plume.util;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class SafeBlockFinder {

    @Getter private int searchRadius = 5;
    @Getter private int multiplier = 4;
    @Getter private Function<Location3i, Double> blockWeightFunction = location -> {
        Block block = location.getWorld().getBlockState(new BlockPos(location.getX(), location.getY(), location.getZ())).getBlock();
        Material material = block.getMaterial();
        if (block instanceof BlockLeaves) {
            return null;
        } else if (material.isSolid()) {
            return (double) 0;
        } else if (material.isLiquid()) {
            return (double) -100;
        } else {
            return null;
        }
    };

    public void setHorizontalSearchRadius(int searchRadius) {
        checkArgument(searchRadius >= 0, "searchRadius >= 0");
        this.searchRadius = searchRadius;
    }

    public void setSearchRadiusMultiplier(int searchRadiusMultiplier) {
        checkArgument(searchRadiusMultiplier >= 0, "searchRadiusMultiplier >= 0");
        this.multiplier = searchRadiusMultiplier;
    }

    public void setBlockWeightFunction(Function<Location3i, Double> blockWeightFunction) {
        checkNotNull(blockWeightFunction, "blockWeightFunction");
        this.blockWeightFunction = blockWeightFunction;
    }

    public List<WeightedEntry<Location3i>> getWeightedLocations(Location3i location) throws NoSafeLocationException {
        List<WeightedEntry<Location3i>> candidates = Lists.newArrayList();
        World world = location.getWorld();
        for (int x = location.getX() - searchRadius * multiplier; x <= location.getX() + searchRadius * multiplier; x += multiplier) {
            for (int z = location.getZ() - searchRadius * multiplier; z <= location.getZ() + searchRadius * multiplier; z += multiplier) {
                int skyY = world.getHeight(new BlockPos(x, 0, z)).getY();
                if (skyY == 0) { // Sometimes the height value is incorrectly returned as 0
                    skyY = world.getHeight();
                }
                for (int y = skyY; y >= 0; y--) {
                    Location3i candidate = new Location3i(location.getWorld(), x, y + 1, z);
                    Double weight = blockWeightFunction.apply(candidate);
                    if (weight != null) {
                        // Penalize locations that don't consist of an open space
                        if (location.getWorld().getBlockState(new BlockPos(x, y + 1, z)) != Blocks.air || location.getWorld().getBlockState(new BlockPos(x, y + 2, z)) != Blocks.air) {
                            weight -= 400;
                        }
                        candidates.add(new WeightedEntry<>(candidate.add(0, 1, 0), weight + -candidate.setY(0).distanceSq(location.setY(0))));
                        break;
                    }
                }
            }
        }
        if (candidates.isEmpty()) {
            throw new NoSafeLocationException();
        }
        Collections.sort(candidates);
        return candidates;
    }

    public static class NoSafeLocationException extends Exception {
    }

}
