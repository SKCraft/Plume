package com.skcraft.plume.module;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.util.event.EventBus;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.event.Cause;
import com.skcraft.plume.event.DelegateEvent;
import com.skcraft.plume.event.block.BlockChange;
import com.skcraft.plume.event.block.BreakBlockEvent;
import com.skcraft.plume.event.block.PlaceBlockEvent;
import com.skcraft.plume.event.entity.DamageEntityEvent;
import com.skcraft.plume.util.BlockState;
import com.skcraft.plume.util.Events;
import com.skcraft.plume.util.Location3i;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import li.cil.oc.api.event.RobotAttackEntityEvent;
import li.cil.oc.api.event.RobotBreakBlockEvent;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.event.RobotPlaceBlockEvent;
import li.cil.oc.api.internal.Agent;
import net.minecraft.init.Blocks;

import java.util.List;

@Module(name = "robot-abstraction-listener", hidden = true)
public class RobotAbstractionListener {

    private final EventBus eventBus;
    private final BlockState STONE_STATE = BlockState.create(Blocks.stone);
    private final BlockState AIR_STATE = BlockState.create(Blocks.air);

    @Inject
    public RobotAbstractionListener(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    private Cause createCause(Agent agent) {
        return Cause.create(new UserId(agent.ownerUUID(), agent.ownerName()), agent);
    }

    @SubscribeEvent
    public void onRobotBreak(RobotBreakBlockEvent.Pre event) {
        if (event.world.isRemote) return;

        Location3i location = new Location3i(event.world, event.x, event.y, event.z);
        BlockState current = BlockState.getBlockAndMeta(location);
        List<BlockChange> changes = Lists.newArrayList(new BlockChange(location, current, AIR_STATE));

        Events.postDelegate(eventBus, new BreakBlockEvent(createCause(event.agent), event.world, changes), event);
    }

    @SubscribeEvent
    public void onRobotMoveEvent(RobotMoveEvent.Pre event) {
        if (event.agent.world().isRemote) return;

        Agent agent = event.agent;

        Location3i location = new Location3i(event.agent.world(),
                (int) agent.xPosition() + event.direction.offsetX,
                (int) agent.yPosition() + event.direction.offsetY,
                (int) agent.zPosition() + event.direction.offsetZ);
        BlockState current = BlockState.getBlockAndMeta(location);
        List<BlockChange> changes = Lists.newArrayList(new BlockChange(location, current, STONE_STATE)); // Hack

        Events.postDelegate(eventBus, new PlaceBlockEvent(createCause(event.agent), event.agent.world(), changes), event);
    }

    @SubscribeEvent
    public void onRobotPlace(RobotPlaceBlockEvent.Pre event) {
        if (event.world.isRemote) return;

        Location3i location = new Location3i(event.world, event.x, event.y, event.z);
        BlockState current = BlockState.getBlockAndMeta(location);
        List<BlockChange> changes = Lists.newArrayList(new BlockChange(location, current, STONE_STATE)); // Hack until we get a block

        Events.postDelegate(eventBus, new PlaceBlockEvent(createCause(event.agent), event.world, changes), event);
    }

    @SubscribeEvent
    public void onRobotAttackEntity(RobotAttackEntityEvent event) {
        if (event.target.worldObj.isRemote) return;

        DelegateEvent delegateEvent = new DamageEntityEvent(createCause(event.agent), event.target.worldObj, Lists.newArrayList(event.target));

        Events.postDelegate(eventBus, delegateEvent, event);
    }

}
