package com.skcraft.plume.common.util.cache;

import com.google.common.cache.*;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

class GuavaManagedCache<K, V> implements ManagedCache<K, V> {

    private final ConcurrentMap<K, Future<V>> pinned = Maps.newConcurrentMap();

    /**
     * Weak reference cache on objects. Depends on {@code pinned} and
     * {@code expireCache} to keep strong references.
     */
    private final LoadingCache<K, Future<V>> referenceCache;

    /**
     * Time-based cache used to actually fetch entries.
     */
    private final LoadingCache<K, Future<V>> expireCache;

    GuavaManagedCache(CacheLoader<K, V> loader, int expireDelay, TimeUnit expireTimeUnit) {
        checkNotNull(loader, "loader");
        checkArgument(expireDelay > 0, "expireDelay > 0");
        checkNotNull(expireTimeUnit, "expireTimeUnit");

        referenceCache = CacheBuilder.newBuilder()
                .weakValues()
                .removalListener(new RemovalListener<Object, Object>() {
                    @Override
                    public void onRemoval(RemovalNotification<Object, Object> notification) {
                        if (notification.getKey() != null) {
                            expireCache.invalidate(notification.getKey());
                        }
                    }
                })
                .build(new CacheLoader<K, Future<V>>() {
                    @Override
                    public Future<V> load(K key) throws Exception {
                        try {
                            V user = loader.load(key);
                            return Futures.immediateFuture(user);
                        } catch (Throwable e) {
                            return Futures.immediateFailedFuture(e);
                        }
                    }
                });

        expireCache = CacheBuilder.newBuilder()
                .expireAfterAccess(expireDelay, expireTimeUnit)
                .build(new CacheLoader<K, Future<V>>() {
                    @Override
                    public Future<V> load(K key) {
                        return referenceCache.getUnchecked(key);
                    }
                });
    }

    @Override
    public V pin(K key) throws ExecutionException {
        checkNotNull(key, "key");
        Future<V> future = expireCache.getUnchecked(key);
        pinned.put(key, future);
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public void unpin(K key) {
        checkNotNull(key, "key");
        expireCache.getUnchecked(key); // Reset the access time
        pinned.remove(key);
    }

    @Override
    public V get(K key) throws ExecutionException {
        checkNotNull(key, "key");
        try {
            return expireCache.getUnchecked(key).get();
        } catch (InterruptedException e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public V getQuietly(K key) {
        try {
            return get(key);
        } catch (ExecutionException e) {
            return null;
        }
    }

    @Override
    public V getIfPresent(K key) throws ExecutionException {
        checkNotNull(key, "key");
        try {
            Future<V> future = referenceCache.getIfPresent(key);
            expireCache.getIfPresent(key); // Reset the access time
            return future != null ? future.get() : null;
        } catch (InterruptedException e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public void put(K key, V value) {
        referenceCache.put(key, Futures.immediateFuture(value));
        expireCache.refresh(key); // Reset access time
    }

    @Override
    public void refresh(K key) {
        checkNotNull(key, "key");
        referenceCache.refresh(key);
        expireCache.refresh(key);
    }

}
