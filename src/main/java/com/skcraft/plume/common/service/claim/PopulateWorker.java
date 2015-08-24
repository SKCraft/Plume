package com.skcraft.plume.common.service.claim;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.service.party.Party;
import com.skcraft.plume.common.service.party.PartyCache;
import com.skcraft.plume.common.util.WorldVector3i;
import lombok.Getter;
import lombok.extern.java.Log;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A worker thread that polls for {@link ChunkState}s from a queue in order
 * to update the ChunkStates with fresh data from the database.
 *
 * <p>{@link ClaimCache} creates several {@code PopulateWorker}s to consume
 * the queue of chunk positions to load and then eventually update the cache
 * with the new claim data.</p>
 */
@Log
class PopulateWorker implements Runnable {

    private final ClaimMap claims;
    private final PartyCache parties;
    private final BlockingQueue<ChunkState> queue;
    @Getter private int collectTime = 1000;
    @Getter private int batchMaxSize = 100;

    /**
     * Create a new instance.
     *
     * @param claims The claim database
     * @param parties The party cache
     * @param queue A queue of coordinates to fetch
     */
    public PopulateWorker(ClaimMap claims, PartyCache parties, BlockingQueue<ChunkState> queue) {
        this.claims = checkNotNull(claims, "claims");
        this.parties = checkNotNull(parties, "parties");
        this.queue = checkNotNull(queue, "queue");
    }

    @Override
    public void run() {
        List<ChunkState> batch = Lists.newArrayList(); // Reused after each queue pass
        do {
            try {
                // Give up collecting states to load once batchMaxSize has been
                // reached, or if too much time has elapsed
                do {
                    Queues.drain(queue, batch, batchMaxSize, collectTime, TimeUnit.MILLISECONDS);
                } while (batch.isEmpty());

                try {
                    Map<WorldVector3i, Claim> loaded = claims.findClaimsByPosition(Lists.transform(batch, ChunkState::getPosition));

                    for (ChunkState state : batch) {
                        Claim claim = loaded.get(state.getPosition());

                        if (claim != null) {
                            // TODO: Fetch parties in bulk instead
                            // Currently the cache backing the party cache doesn't support bulk
                            // loads while also blocking other threads from loading the values
                            // themselves
                            String partyName = claim.getParty();
                            Party party = null;
                            if (partyName != null) {
                                party = parties.get(partyName);
                            }

                            state.setClaim(claim, party);
                        }

                        state.setLoaded(true);
                    }
                } catch (DataAccessException e) {
                    queue.addAll(batch); // Re-insert
                    log.log(Level.WARNING, "Failed to read claim information", e);
                }
                batch.clear();
            } catch (Exception e) {
                log.log(Level.WARNING, "An error occurred in the chunk claim population thread", e);
            }
        } while (true);
    }

}
