package com.skcraft.plume.common.service.claim;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.skcraft.plume.common.service.party.Party;
import com.skcraft.plume.common.service.party.PartyCache;
import com.skcraft.plume.common.util.WorldVector3i;
import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.map.hash.TLongObjectHashMap;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Loads claims asynchronously when they are requested, and then later
 * provides that information on demand.
 *
 * <p>Claims are loaded in batches whenever possible to minimize round trips
 * to the underlying database, which does mean that there may be a maximum
 * delay of one second (plus the time it takes for the database response)
 * for claim data that was requested to be made available. Claim data can
 * be requested on a chunk-by-chunk basis using
 * {@link #queueChunk(WorldVector3i)}, which should be called when
 * chunks are being loaded in a world. When chunks are being unloaded
 * from a world, then {@link #invalidateChunk(WorldVector3i)} should be
 * called.</p>
 *
 * <p>If changes are made to claims, then {@link #putClaims(Collection)}
 * can be used to inform the cache of updates. Only claims that have been
 * requested previously (through {@link #queueChunk(WorldVector3i)})
 * will be updated; claims not yet requested will not be cached.</p>
 *
 * <p>Multiple read attempts share a read lock and will minimally block,
 * but writes to the cache (through {@code putClaims()}) require a write
 * lock that will block other threads. However, the update is brief and
 * the block should not last for very long. Asynchronous updates (as
 * data is pulled from the underlying database) do not involve any locks.</p>
 *
 * <p>When the database is unable to retrieve claim data, the attempt
 * will be aborted and no claim data will be available for that chunk. This
 * behavior is subject to change in the future, with extra consideration
 * to prevent the queue from growing exceptionally large as errors
 * accumulate (such as in the case of complete database unavailability).</p>
 */
public class ClaimCache {

    /**
     * The default number of worker threads to use to load claim data
     * asynchronously.
     */
    public static final int DEFAULT_WORKER_COUNT = 2;

    private final LoadingCache<String, TLongObjectHashMap<ChunkState>> statesByWorld = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, TLongObjectHashMap<ChunkState>>() {
                @Override
                public TLongObjectHashMap<ChunkState> load(@Nullable String key) throws Exception {
                    return new TLongObjectHashMap<>();
                }
            });

    @Getter
    private final ClaimMap claimMap;
    private final PartyCache parties;
    private final BlockingQueue<ChunkState> statePopulateQueue = new LinkedBlockingQueue<>();
    private final ReadWriteLock stateLock = new ReentrantReadWriteLock();

    /**
     * Create a new instance with {@code DEFAULT_WORKER_COUNT} worker threads.
     *
     * @param claims The underlying claim database
     * @param parties The party cache
     */
    public ClaimCache(ClaimMap claims, PartyCache parties) {
        this(claims, parties, DEFAULT_WORKER_COUNT);
    }

    /**
     * Create a new instance.
     *
     * @param claims The underlying claim database
     * @param parties The party cache
     * @param workerThreadCount The number of worker threads to use to load claims asynchronously
     */
    public ClaimCache(ClaimMap claims, PartyCache parties, int workerThreadCount) {
        checkNotNull(claims, "claims");
        checkNotNull(parties, "parties");
        checkArgument(workerThreadCount >= 1, "workerThreadCount >= 1");
        this.claimMap = claims;
        this.parties = parties;

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Plume Chunk State Worker #%d").build();

        for (int i = 0; i < workerThreadCount; i++) {
            threadFactory.newThread(new PopulateWorker(claims, parties, statePopulateQueue)).start();
        }
    }

    /**
     * Request claim data for the given position.
     *
     * <p>The method will submit a request to the queue but will not wait until
     * the data has been fetched.</p>
     *
     * @param position The position of the chunk
     */
    public void queueChunk(WorldVector3i position) {
        checkNotNull(position, "position");
        Lock lock = stateLock.writeLock();
        try {
            lock.lock();
            TLongObjectHashMap<ChunkState> states = statesByWorld.getUnchecked(position.getWorldId());
            long key = toLong(position);
            ChunkState state = states.get(key);
            if (state == null) {
                state = new ChunkState(position);
                states.put(key, state);
                statePopulateQueue.add(state);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Remove cached claim data for the given position.
     *
     * @param position The position of the chunk
     */
    public void invalidateChunk(WorldVector3i position) {
        checkNotNull(position, "position");
        Lock lock = stateLock.writeLock();
        try {
            lock.lock();
            TLongObjectHashMap<ChunkState> states = statesByWorld.getUnchecked(position.getWorldId());
            states.remove(toLong(position));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removed cache claim data for all chunks in a given world.
     *
     * @param worldName The world name
     */
    public void invalidateChunksInWorld(String worldName) {
        checkNotNull(worldName, "worldName");
        Lock lock = stateLock.writeLock();
        try {
            lock.lock();
            statesByWorld.invalidate(worldName);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get a claim for the given position if it has been cached.
     *
     * <p>This method is the recommended method to use to get claim data
     * while the world is ticking.</p>
     *
     * @param position The position of the chunk
     * @return The claim entry, otherwise null
     */
    @Nullable
    public ClaimEntry getClaimIfPresent(WorldVector3i position) {
        checkNotNull(position, "position");
        Lock lock = stateLock.readLock();
        try {
            lock.lock();
            TLongObjectHashMap<ChunkState> states = statesByWorld.getUnchecked(position.getWorldId());
            long key = toLong(position);
            ChunkState state = states.get(key);
            if (state == null) {
                return null;
            } else {
                return state.isLoaded() ? state : null;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Re-fetch claim data for all the chunks that are currently in the cache,
     * not including chunks that are still pending load.
     */
    public void refreshAllClaims() {
        Lock lock = stateLock.writeLock();
        try {
            lock.lock();
            for (TLongObjectHashMap<ChunkState> states : statesByWorld.asMap().values()) {
                TLongObjectIterator<ChunkState> it = states.iterator();
                while (it.hasNext()) {
                    it.advance();
                    ChunkState state = it.value();
                    if (state.isLoaded()) {
                        statePopulateQueue.add(state);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Write the given claim data to the cache, only writing data for claims
     * that are already in the cache or are pending load.
     *
     * @param claims The claims
     */
    public void putClaims(Collection<Claim> claims) {
        checkNotNull(claims, "claims");
        Lock lock = stateLock.writeLock();
        try {
            lock.lock();
            for (Claim claim : claims) {
                if (claim != null) {
                    WorldVector3i position = new WorldVector3i(claim.getWorld(), claim.getX(), 0, claim.getZ());
                    TLongObjectHashMap<ChunkState> states = statesByWorld.getUnchecked(position.getWorldId());

                    String partyName = claim.getParty();
                    Party party = null;
                    if (partyName != null) {
                        party = parties.get(partyName);
                    }

                    ChunkState state = new ChunkState(position);
                    state.setClaim(claim, party);
                    state.setLoaded(true);
                    states.put(toLong(position), state);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Set the given positions as unclaimed in the cache if those positions
     * are already cached.
     *
     * @param positions The positions
     */
    public void putAsUnclaimed(Collection<WorldVector3i> positions) {
        checkNotNull(positions, "positions");
        Lock lock = stateLock.writeLock();
        try {
            lock.lock();
            for (WorldVector3i position : positions) {
                if (position != null) {
                    TLongObjectHashMap<ChunkState> states = statesByWorld.getUnchecked(position.getWorldId());
                    ChunkState state = new ChunkState(position);
                    state.setLoaded(true);
                    states.put(toLong(position), state);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    protected static long toLong(WorldVector3i position) {
        int msw = position.getX();
        int lsw = position.getZ();
        return ((long) msw << 32) + lsw - Integer.MIN_VALUE;
    }

}
