package com.skcraft.plume.event.block;

import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.skcraft.plume.event.BulkEvent;
import com.skcraft.plume.event.Cause;
import com.skcraft.plume.event.DelegateEvent;
import com.skcraft.plume.event.Result;
import com.skcraft.plume.util.Location3i;
import net.minecraft.world.World;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

abstract class BlockEvent extends DelegateEvent implements BulkEvent {

    private final World world;

    protected BlockEvent(Cause cause, World world) {
        super(cause);
        checkNotNull(world, "world");
        this.world = world;
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
     * Get a list of affected locations.
     *
     * @return A list of affected locations
     */
    public abstract List<Location3i> getLocations();

    /**
     * Filter the list of affected blocks with the given predicate. If the
     * predicate returns {@code false}, then the block is removed.
     *
     * @param predicate the predicate
     * @param cancelEventOnFalse true to cancel the event and clear the block
     *                           list once the predicate returns {@code false}
     * @return Whether one or more blocks were filtered out
     */
    public boolean filterLocations(Predicate<Location3i> predicate, boolean cancelEventOnFalse) {
        return filter(getLocations(), Functions.<Location3i>identity(), predicate, cancelEventOnFalse);
    }

    @Override
    public Result getResult() {
        if (getLocations().isEmpty()) {
            return Result.DENY;
        }
        return super.getResult();
    }

    @Override
    public Result getExplicitResult() {
        return super.getResult();
    }

}
