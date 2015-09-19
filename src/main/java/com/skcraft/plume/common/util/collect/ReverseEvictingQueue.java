package com.skcraft.plume.common.util.collect;

import com.google.common.collect.ForwardingQueue;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Queue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class ReverseEvictingQueue<E> extends ForwardingQueue<E> implements Serializable {

    private final Deque<E> delegate;
    private final int maxSize;

    private ReverseEvictingQueue(int maxSize) {
        checkArgument(maxSize >= 0, "maxSize >= 0", maxSize);
        this.delegate = new ArrayDeque<>(maxSize);
        this.maxSize = maxSize;
    }

    @Override
    protected Queue<E> delegate() {
        return delegate;
    }

    @Override
    public boolean offer(E e) {
        return add(e);
    }

    @Override
    public boolean add(E e) {
        if (maxSize == 0) {
            return true;
        }
        if (size() == maxSize) {
            delegate.removeLast();
        }
        delegate.offerFirst(e);
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        return standardAddAll(collection);
    }

    @Override
    public boolean contains(Object object) {
        return delegate().contains(checkNotNull(object));
    }

    @Override
    public boolean remove(Object object) {
        return delegate().remove(checkNotNull(object));
    }

    public int remainingCapacity() {
        return maxSize - size();
    }

    public static <E> ReverseEvictingQueue<E> create(int maxSize) {
        return new ReverseEvictingQueue<E>(maxSize);
    }

}
