package com.skcraft.plume.listener;

import com.google.common.collect.Lists;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.skcraft.plume.event.Cause;
import com.skcraft.plume.event.DelegateEvent;
import com.skcraft.plume.event.block.BlockChange;
import com.skcraft.plume.event.block.BreakBlockEvent;
import com.skcraft.plume.event.block.PlaceBlockEvent;
import com.skcraft.plume.event.block.UseBlockEvent;
import com.skcraft.plume.event.entity.DamageEntityEvent;
import com.skcraft.plume.event.entity.UseEntityEvent;
import com.skcraft.plume.util.BlockState;
import com.skcraft.plume.util.Events;
import com.skcraft.plume.util.Location3i;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;

import java.util.List;

public class EventAbstractionListener {

    private final EventBus eventBus;
    private final BlockState AIR_STATE = BlockState.create(Blocks.air);

    public EventAbstractionListener(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    private static Location3i getLocation3i(BlockEvent event) {
        return new Location3i(event.world, event.x, event.y, event.z);
    }

    //-------------------------------------------------------------------------
    // Block break / place
    //-------------------------------------------------------------------------

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        if (event.world.isRemote) return;

        Location3i location = getLocation3i(event);
        BlockState current = BlockState.create(event.block, event.blockMetadata);
        List<BlockChange> changes = Lists.newArrayList(new BlockChange(location, current, AIR_STATE));

        Events.postDelegate(eventBus, new BreakBlockEvent(Cause.create(event.getPlayer()), event.getPlayer().worldObj, changes), event);
    }

    @SubscribeEvent
    public void onPlace(BlockEvent.PlaceEvent event) {
        if (event.world.isRemote) return;

        Location3i location = getLocation3i(event);
        BlockState current = BlockState.create(event.block, event.blockMetadata);
        List<BlockChange> changes = Lists.newArrayList(new BlockChange(location, current, BlockState.wrap(event.blockSnapshot)));

        Events.postDelegate(eventBus, new PlaceBlockEvent(Cause.create(event.player), event.player.worldObj, changes), event);
    }

    @SubscribeEvent
    public void onMultiPlace(BlockEvent.MultiPlaceEvent event) {
        if (event.world.isRemote) return;

        List<BlockChange> changes = Lists.newArrayList();
        for (BlockSnapshot snapshot : event.getReplacedBlockSnapshots()) {
            changes.add(new BlockChange(Location3i.fromBlockSnapshot(snapshot), BlockState.create(snapshot.getCurrentBlock()), BlockState.wrap(snapshot)));
        }

        Events.postDelegate(eventBus, new PlaceBlockEvent(Cause.create(event.player), event.player.worldObj, changes), event);
    }

    //-------------------------------------------------------------------------
    // Block external interaction
    //-------------------------------------------------------------------------

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.world.isRemote) return;

        switch (event.action) {
            case LEFT_CLICK_BLOCK:
            case RIGHT_CLICK_BLOCK:
                if (event.useBlock != Result.DENY) {
                    Location3i location = new Location3i(event.world, event.x, event.y, event.z);
                    Block block = event.world.getBlock(location.getX(), location.getY(), location.getZ());

                    if (block == Blocks.air) {
                        location = new Location3i(event.world, (int) event.entityPlayer.posX, (int) event.entityPlayer.posY, (int) event.entityPlayer.posZ);
                        block = event.world.getBlock(location.getX(), location.getY(), location.getZ());
                    }

                    if (block != Blocks.air) {
                        List<Location3i> locations = Lists.newArrayList(location);

                        UseBlockEvent useBlockEvent = new UseBlockEvent(Cause.create(event.entityPlayer), event.entityPlayer.worldObj, locations);
                        if (Events.postDelegate(eventBus, useBlockEvent, event)) {
                            event.useBlock = Result.DENY;
                        }
                    }
                }
                break;
        }
    }

    @SubscribeEvent
    public void onBucketFill(FillBucketEvent event) {
        if (event.world.isRemote) return;

        Location3i location = Location3i.fromObjectPosition(event.world, event.target);
        BlockState current = BlockState.getBlockAndMeta(location);
        List<BlockChange> changes = Lists.newArrayList(new BlockChange(location, current, AIR_STATE));

        Events.postDelegate(eventBus, new BreakBlockEvent(Cause.create(event.entityPlayer), event.entityPlayer.worldObj, changes), event);
    }

    @SubscribeEvent
    public void onPlayerAttackEntityEvent(AttackEntityEvent event) {
        if (event.entityPlayer.worldObj.isRemote) return;

        DelegateEvent delegateEvent = new DamageEntityEvent(Cause.create(event.entityPlayer), event.entityPlayer.worldObj, Lists.newArrayList(event.target));

        Events.postDelegate(eventBus, delegateEvent, event);
    }

    //-------------------------------------------------------------------------
    // Entity external interaction
    //-------------------------------------------------------------------------

    @SubscribeEvent
    public void onEntityInteract(EntityInteractEvent event) {
        if (event.entityPlayer.worldObj.isRemote) return;

        DelegateEvent delegateEvent = new UseEntityEvent(Cause.create(event.entityPlayer), event.entityPlayer.worldObj, Lists.newArrayList(event.entity));
        Events.postDelegate(eventBus, delegateEvent, event);
    }

}
