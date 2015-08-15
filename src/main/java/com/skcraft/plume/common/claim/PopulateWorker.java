package com.skcraft.plume.common.claim;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.party.Party;
import com.skcraft.plume.common.party.PartyCache;
import com.skcraft.plume.common.util.WorldVector3i;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A worker thread that polls for {@link ChunkState}s from a queue in order
 * to update the ChunkStates with fresh data from the database.
 *
 * <p>{@link ClaimCache} creates several {@code PopulateWorker}s to consume
 * the queue of chunk positions to load and then eventually update the cache
 * with the new claim data.</p>
 */
@Slf4j
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
                Queues.drain(queue, batch, batchMaxSize, collectTime, TimeUnit.MILLISECONDS);

                Map<WorldVector3i, Claim> loaded = claims.findClaimsByPosition(Lists.transform(batch, ChunkState::getPosition));

                for (ChunkState state : batch) {
                    try {
                        Claim claim = loaded.get(state.getPosition());

                        // TODO: Fetch parties in bulk instead
                        // Currently the cache backing the party cache doesn't support bulk
                        // loads while also blocking other threads from loading the values
                        // themselves
                        String partyName = claim.getParty();
                        Party party = null;
                        if (partyName != null) {
                            party = parties.getParty(partyName);
                        }

                        state.setClaim(claim, party);
                        state.setLoaded(true);
                    } catch (DataAccessException e) {
                        // Should perhaps add a back off option, but it needs to also
                        // not result in a huge queue due to database unavailability
                        log.warn("Failed to read claim information for " + state.getPosition());
                    }
                }
                batch.clear();
            } catch (Exception e) {
                log.warn("An error occurred in the chunk claim population thread", e);
            }
        } while (true);
    }

}
