package com.skcraft.plume.common.journal;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import lombok.Getter;
import lombok.extern.java.Log;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A worker that saves records to an underlying journal in the
 * background.
 */
@Log
class RecordSaveWorker implements Runnable {

    private final Journal journal;
    private final BlockingQueue<Record> queue;
    private final AtomicBoolean run = new AtomicBoolean(true);
    @Getter private int collectTime = 1000;
    @Getter private int batchMaxSize = 100;
    private final Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
            .retryIfException()
            .withWaitStrategy(WaitStrategies.fibonacciWait(2, TimeUnit.MINUTES))
            .build();

    RecordSaveWorker(Journal journal, BlockingQueue<Record> queue) {
        checkNotNull(journal, "journal");
        checkNotNull(queue, "queue");
        this.journal = journal;
        this.queue = queue;
    }

    public void shutdown() {
        if (!run.compareAndSet(true, false)) {
            log.warning("Tried to stop RecordSaveWorker more than once");
        }
    }

    @Override
    public void run() {
        List<Record> pending = Lists.newArrayList();

        // Repeatedly drain from the queue
        while (run.get()) {
            try {
                Queues.drain(queue, pending, batchMaxSize, collectTime, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
                if (pending.size() < batchMaxSize) {
                    continue; // Start draining again
                }
            }

            try {
                retryer.call(() -> { journal.addRecords(pending); return true; });
            } catch (ExecutionException | RetryException e) {
                log.log(Level.WARNING, "Failed to retry adding records to the journal, which should because retries should continue forever", e);
            }

            pending.clear();
        }
    }

}
