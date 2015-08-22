/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.plume.common.util.concurrent;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.concurrent.Callable;

/**
 * An extension of {@link ListenableFuture} that provides convenience methods
 * to register functions that are triggered after upon the completion of
 * the underlying task.
 *
 * <p>Dependent functions are executed using the "default" executor which
 * is specified when {@code Deferred} is first created, unless
 * the async variants are used to register the function.</p>
 *
 * @param <I> The type returned
 */
public interface Deferred<I> extends ListenableFuture<I> {

    <O> Deferred<O> thenRun(Callable<O> task);

    <O> Deferred<O> thenRun(Callable<O> task, ListeningExecutorService executor);

    Deferred<Void> thenRun(Runnable task);

    Deferred<Void> thenRun(Runnable task, ListeningExecutorService executor);

    Deferred<Void> then(Callback<I> task);

    Deferred<Void> then(Callback<I> task, ListeningExecutorService executor);

    Deferred<I> tap(Runnable task);

    Deferred<I> tap(Runnable task, ListeningExecutorService executor);

    <O> Deferred<O> filter(Filter<I, O> function);

    <O> Deferred<O> filter(Filter<I, O> function, ListeningExecutorService executor);

    Deferred<I> done(Callback<I> onSuccess);

    Deferred<I> done(Callback<I> onSuccess, ListeningExecutorService executor);

    Deferred<I> fail(Callback<Throwable> onFailure);

    Deferred<I> fail(Callback<Throwable> onFailure, ListeningExecutorService executor);

}
