package com.skcraft.plume.event.block;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.skcraft.plume.event.Cause;
import com.skcraft.plume.util.Location3i;
import net.minecraft.world.World;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class ChangeBlockEvent extends BlockEvent {

    private static final ChangeLocationFunction CHANGE_LOCATION_FUNCTION = new ChangeLocationFunction();
    private final List<BlockChange> changeList;

    public ChangeBlockEvent(Cause cause, World world, List<BlockChange> changeList) {
        super(cause, world);
        checkNotNull(changeList, "changeList");
        this.changeList = changeList;
    }

    @Override
    public List<Location3i> getLocations() {
        return Lists.transform(changeList, CHANGE_LOCATION_FUNCTION);
    }

    /**
     * Get the list of changed blocks.
     *
     * @return The list of changed blocks
     */
    public List<BlockChange> getChanges() {
        return changeList;
    }

    /**
     * Filter the list of affected blocks with the given predicate. If the
     * predicate returns {@code false}, then the block is removed.
     *
     * @param predicate the predicate
     * @param cancelEventOnFalse true to cancel the event and clear the block
     *                           list once the predicate returns {@code false}
     * @return True if one or more blocks were filtered out
     */
    public boolean filterChanges(Predicate<BlockChange> predicate, boolean cancelEventOnFalse) {
        return filter(changeList, Functions.<BlockChange>identity(), predicate, cancelEventOnFalse);
    }

    private static class ChangeLocationFunction implements Function<BlockChange, Location3i> {
        @Override
        public Location3i apply(BlockChange input) {
            return input.getLocation();
        }
    }
}
