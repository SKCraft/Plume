package com.skcraft.plume.module.claim;

import com.google.common.base.Function;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.regions.Region;
import com.skcraft.plume.common.util.WorldVector3i;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.skcraft.plume.common.util.SharedLocale.tr;

public class ClaimEnumerator {

    @Getter @Setter
    private WorldVector3i spawn = new WorldVector3i("world", 0, 0, 0);
    private final ClaimConfig config;
    @Getter @Setter
    private boolean limited = true;

    public ClaimEnumerator(ClaimConfig config) {
        checkNotNull(config, "config");
        this.config = config;
    }

    public <T> List<T> enumerate(Region selection, Function<Vector2D, T> function) throws ClaimAttemptException {
        // Check the length of the claim in one direction
        if (limited && (selection.getWidth() > config.limits.chunkLengthCountMax * 16 || selection.getLength() > config.limits.chunkLengthCountMax * 16)) {
            throw new ClaimAttemptException(tr("claims.chunkLengthCountMaxExceeded", config.limits.chunkLengthCountMax));
        }

        Set<Vector2D> coords = selection.getChunks();

        // Check the minimum list of chunks
        if (coords.isEmpty()) {
            throw new ClaimAttemptException(tr("claims.noChunksSelected"));
        }

        // Check number of selected chunks
        if (limited && coords.size() > config.limits.claimTaskMax) {
            throw new ClaimAttemptException(tr("claims.tooManyChunksAtATime", config.limits.claimTaskMax));
        }

        // Change coordinates into chunks
        List<T> results = new ArrayList<>();
        for (Vector2D coord : coords) {
            double distanceSq = Math.pow(coord.getX() * 16 - spawn.getX(), 2) + Math.pow(coord.getZ() * 16 - spawn.getZ(), 2);

            // Is the chunk too far away from origin?
            if (limited && distanceSq > config.limits.distanceFromSpawnMax * config.limits.distanceFromSpawnMax) {
                throw new ClaimAttemptException(tr("claims.tooCloseToSpawn"));
            }

            // Is the chunk too close to origin?
            if (limited && distanceSq < config.limits.distanceFromSpawnMin * config.limits.distanceFromSpawnMin) {
                throw new ClaimAttemptException(tr("claims.tooFarFromSpawn"));
            }


            results.add(function.apply(coord));
        }

        return results;
    }

}
