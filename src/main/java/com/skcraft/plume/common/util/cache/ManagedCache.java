package com.skcraft.plume.common.util.cache;

import java.util.concurrent.ExecutionException;

/**
 * A cache that naturally evicts entries that have not been accessed in a
 * certain length of time, unless an entry has been "pinned" so that it is
 * not ever evicted automatically until it is "unpinned."
 *
 * <p>Failures that occur when a value is loaded are also cached so as to
 * not to attempt another load so soon. Exceptions that are thrown during
 * fetching will be thrown when any of the cache retrieval methods are
 * called.</p>
 *
 * @param <K> They key type
 * @param <V> THe value type
 */
public interface ManagedCache<K, V> {

    /**
     * Pin the value for the given key, after loading the requested object
     * if it has not yet been loaded.
     *
     * <p>This method may block to fetch the value.</p>
     *
     * @param key The key
     * @return The value
     * @throws ExecutionException Thrown when load fails
     */
    V pin(K key) throws ExecutionException;

    /**
     * Unpin the entry for the given key.
     *
     * @param key The key
     */
    void unpin(K key);

    /**
     * Fetch a value for the given key from the cache or through the cache
     * loader.
     *
     * @param key The key
     * @return The value
     * @throws ExecutionException Thrown when load fails
     */
    V get(K key) throws ExecutionException;

    /**
     * Fetch a value for the given key from the cache or through the cache
     * loader, consuming exceptions.
     *
     * @param key The key
     * @return The value
     */
    V getQuietly(K key);

    /**
     * Get a value only if it already exists in the cache.
     *
     * @param key The key
     * @return The value
     * @throws ExecutionException Thrown when load fails
     */
    V getIfPresent(K key) throws ExecutionException;

    /**
     * Replace an entry in the cache with the given value.
     *
     * @param key The key
     * @param value The value
     */
    void put(K key, V value);

    /**
     * Refresh the given key.
     *
     * @param key The key
     */
    void refresh(K key);

}
