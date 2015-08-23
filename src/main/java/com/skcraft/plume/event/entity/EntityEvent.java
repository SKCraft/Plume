package com.skcraft.plume.event.entity;

import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.skcraft.plume.event.BulkEvent;
import com.skcraft.plume.event.Cause;
import com.skcraft.plume.event.DelegateEvent;
import com.skcraft.plume.event.Result;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class EntityEvent extends DelegateEvent implements BulkEvent {

    private final World world;
    private final List<Entity> entities;

    protected EntityEvent(Cause cause, World world, List<Entity> entities) {
        super(cause);
        checkNotNull(world);
        checkNotNull(entities);
        this.world = world;
        this.entities = entities;
    }

    /**
     * Get the world.
     *
     * @return The world
     */
    public World getWorld() {
        return world;
    }

    /**
     * Get the affected entities.
     *
     * @return A list of affected entities
     */
    public List<Entity> getEntities() {
        return entities;
    }

    /**
     * Filter the list of affected entities with the given predicate. If the
     * predicate returns {@code false}, then the entity is removed.
     *
     * @param predicate the predicate
     * @param cancelEventOnFalse true to cancel the event and clear the entity
     *                           list once the predicate returns {@code false}
     * @return True if one or more entities were filtered out
     */
    public boolean filterEntities(Predicate<Entity> predicate, boolean cancelEventOnFalse) {
        return filter(getEntities(), Functions.<Entity>identity(), predicate, cancelEventOnFalse);
    }

    @Override
    public Result getResult() {
        if (entities.isEmpty()) {
            return Result.DENY;
        }
        return super.getResult();
    }

    @Override
    public Result getExplicitResult() {
        return super.getResult();
    }

}
