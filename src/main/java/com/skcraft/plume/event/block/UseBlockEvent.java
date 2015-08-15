package com.skcraft.plume.event.block;

import com.skcraft.plume.event.Cause;
import com.skcraft.plume.util.Location3i;
import net.minecraft.world.World;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class UseBlockEvent extends BlockEvent {

    private final List<Location3i> locations;

    public UseBlockEvent(Cause cause, World world, List<Location3i> locations) {
        super(cause, world);
        checkNotNull(locations, "locations");
        this.locations = locations;
    }

    @Override
    public List<Location3i> getLocations() {
        return locations;
    }

}
