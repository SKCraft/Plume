/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.plume.common.util.concurrent;

import com.google.common.base.Function;
import com.google.common.util.concurrent.*;
import lombok.extern.java.Log;

import java.util.concurrent.*;
import java.util.logging.Level;

@Log
class DeferredImpl<I> implements Deferred<I> {

    private final ListenableFuture<I> future;
    private final ListeningExecutorService defaultExecutor;

    DeferredImpl(ListenableFuture<I> future, ListeningExecutorService defaultExecutor) {
        this.future = future;
        this.defaultExecutor = defaultExecutor;
    }

    @Override
    public <O> Deferred<O> thenRun(Callable<O> task) {
        return thenRun(task, defaultExecutor);
    }

    @Override
    public <O> Deferred<O> thenRun(final Callable<O> task, ListeningExecutorService executor) {
        return new DeferredImpl<>(Futures.transform(future,  (AsyncFunction<I, O>) input -> Futures.immediateFuture(task.call()), executor), defaultExecutor);
    }

    @Override
    public Deferred<Void> thenRun(Runnable task) {
        return thenRun(task, defaultExecutor);
    }

    @Override
    public Deferred<Void> thenRun(final Runnable task, ListeningExecutorService executor) {
        return new DeferredImpl<Void>(Futures.transform(future, new Function<I, Void>() {
            @Override
            public Void apply(I input) {
                task.run();
                return null;
            }
        }), defaultExecutor);
    }

    @Override
    public Deferred<Void> then(Callback<I> task) {
        return then(task, defaultExecutor);
    }

    @Override
    public Deferred<Void> then(Callback<I> task, ListeningExecutorService executor) {
        return new DeferredImpl<>(Futures.transform(future, (AsyncFunction<I, Void>) input -> {
            task.handle(input);
            return Futures.immediateFuture(null);
        }, executor), defaultExecutor);
    }

    @Override
    public Deferred<I> tap(Runnable task) {
        return tap(task, defaultExecutor);
    }

    @Override
    public Deferred<I> tap(final Runnable task, ListeningExecutorService executor) {
        return filter(input -> {
            task.run();
            return input;
        }, executor);
    }

    @Override
    public <O> Deferred<O> filter(Filter<I, O> function) {
        return filter(function, defaultExecutor);
    }

    @Override
    public <O> Deferred<O> filter(Filter<I, O> function, ListeningExecutorService executor) {
        return new DeferredImpl<O>(Futures.transform(future, (AsyncFunction<I, O>) input -> Futures.immediateFuture(function.apply(input)), executor), defaultExecutor);
    }

    @Override
    public Deferred<I> done(Callback<I> onSuccess) {
        return done(onSuccess, defaultExecutor);
    }

    @Override
    public Deferred<I> done(Callback<I> onSuccess, ListeningExecutorService executor) {
        Futures.addCallback(future, new FutureCallback<I>() {
            @Override
            public void onSuccess(I result) {
                try {
                    onSuccess.handle(result);
                } catch (Exception e) {
                    log.log(Level.WARNING, "Exception thrown during callback", e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
            }
        }, executor);

        return this;
    }

    @Override
    public Deferred<I> fail(Callback<Throwable> onFailure) {
        return fail(onFailure, defaultExecutor);
    }

    @Override
    public Deferred<I> fail(Callback<Throwable> onFailure, ListeningExecutorService executor) {
        Futures.addCallback(future, new FutureCallback<I>() {
            @Override
            public void onSuccess(I result) {
            }

            @Override
            public void onFailure(Throwable t) {
                try {
                    onFailure.handle(t);
                } catch (Exception e) {
                    log.log(Level.WARNING, "Exception thrown during callback", e);
                }
            }
        }, executor);

        return this;
    }

    @Override
    public void addListener(Runnable listener, Executor executor) {
        future.addListener(listener, executor);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    public I get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    public I get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }

}
