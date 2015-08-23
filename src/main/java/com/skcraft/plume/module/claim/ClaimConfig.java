package com.skcraft.plume.module.claim;

import com.skcraft.plume.util.inventory.Inventories;
import com.skcraft.plume.util.inventory.SingleItemMatcher;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

public class ClaimConfig {

    @Setting
    public Cost cost = new Cost();

    @Setting
    public ClaimLimits limits = new ClaimLimits();

    @Setting
    public Protection protection = new Protection();

    @ConfigSerializable
    public static class Cost {

        @Setting(comment = "The number of chunks that a user may own for free before claims have a cost")
        public int gratisChunkCount = 25;

        @Setting(comment = "The item required to buy a chunk")
        public SingleItemMatcher item = Inventories.typeDamageMatcher(new ItemStack(Items.coal));

        @Setting(comment = "The item count cost of each chunk")
        public int itemAmount = 1;

    }

    @ConfigSerializable
    public static class ClaimLimits {

        @Setting(comment = "The maximum distance from spawn that the claim can be made")
        public int distanceFromSpawnMax = 20000;

        @Setting(comment = "The minimum distance from spawn that the claim can be made")
        public int distanceFromSpawnMin = 200;

        @Setting(comment = "The maximum number of chunks in one dimension that can be claimed at one time")
        public int chunkLengthCountMax = 10;

        @Setting(comment = "The maximum number of chunks that can be claimed at one time")
        public int claimTaskMax = 100;

        @Setting(comment = "The maximum number of chunks that can be claimed in total by one person")
        public int totalClaimsMax = 400;

    }

    @ConfigSerializable
    public static class Protection {

        @Setting(comment = "Whether fake players should be given free reign to do whatever they want")
        public boolean ignoreFakePlayers = true;

    }

}
