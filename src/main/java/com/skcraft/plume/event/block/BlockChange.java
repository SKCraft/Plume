package com.skcraft.plume.event.block;

import com.skcraft.plume.util.BlockState;
import com.skcraft.plume.util.Location3i;

import static com.google.common.base.Preconditions.checkNotNull;

public class BlockChange {

    private final Location3i location;
    private final BlockState current;
    private final BlockState replacement;

    public BlockChange(Location3i location, BlockState current, BlockState replacement) {
        checkNotNull(location, "location");
        checkNotNull(current, "current");
        checkNotNull(replacement, "replacement");
        this.location = location;
        this.current = current;
        this.replacement = replacement;
    }

    public Location3i getLocation() {
        return location;
    }

    public BlockState getCurrent() {
        return current;
    }

    public BlockState getReplacement() {
        return replacement;
    }

}
