package com.skcraft.plume.module.claim;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.skcraft.plume.common.service.claim.Claim;
import com.skcraft.plume.common.service.claim.ClaimMap;
import com.skcraft.plume.common.util.WorldVector3i;
import com.skcraft.plume.module.perf.profiler.Appender;
import com.skcraft.plume.module.perf.profiler.Timing;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClaimAppender implements Appender {

    private final Map<WorldVector3i, Claim> cache;

    public ClaimAppender(Collection<Timing> timings, ClaimMap claimMap) {
        Set<WorldVector3i> locations = Sets.newHashSet();
        for (Timing timing : timings) {
            locations.add(new WorldVector3i(timing.getWorld(), timing.getX() >> 4, 0, timing.getZ() >> 4));
        }
        if (!locations.isEmpty()) {
            cache = claimMap.findClaimsByPosition(locations);
        } else {
            cache = Maps.newHashMap();
        }
    }

    @Override
    public List<String> getColumns() {
        return Lists.newArrayList("OwnerUUID", "Owner", "Party");
    }

    @Override
    public List<String> getValues(Timing timing) {
        WorldVector3i location = new WorldVector3i(timing.getWorld(), timing.getX() >> 4, 0, timing.getZ() >> 4);
        Claim claim = cache.get(location);
        if (claim != null) {
            return Lists.newArrayList(claim.getOwner().getUuid().toString(), claim.getOwner().getName(), Strings.nullToEmpty(claim.getParty()));
        } else {
            return Lists.newArrayList("", "", "");
        }
    }
}
