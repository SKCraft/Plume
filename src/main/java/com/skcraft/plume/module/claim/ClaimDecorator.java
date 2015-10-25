package com.skcraft.plume.module.claim;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.skcraft.plume.common.service.claim.Claim;
import com.skcraft.plume.common.service.claim.ClaimMap;
import com.skcraft.plume.common.util.WorldVector3i;
import com.skcraft.plume.event.report.Decorator;
import com.skcraft.plume.event.report.Row;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClaimDecorator implements Decorator {

    private final Map<WorldVector3i, Claim> cache;

    public ClaimDecorator(Collection<? extends Row> rows, ClaimMap claimMap) {
        Set<WorldVector3i> locations = Sets.newHashSet();
        for (Row row : rows) {
            locations.add(new WorldVector3i(row.getWorld(), row.getChunkX(), 0, row.getChunkZ()));
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
    public List<String> getValues(Row row) {
        WorldVector3i location = new WorldVector3i(row.getWorld(), row.getChunkX(), 0, row.getChunkZ());
        Claim claim = cache.get(location);
        if (claim != null) {
            return Lists.newArrayList(claim.getOwner().getUuid().toString(), claim.getOwner().getName(), Strings.nullToEmpty(claim.getParty()));
        } else {
            return Lists.newArrayList("", "", "");
        }
    }
}
