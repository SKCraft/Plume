package com.skcraft.plume.common.service.journal;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import lombok.Getter;
import lombok.extern.java.Log;
import org.mapdb.DB;
import org.mapdb.TxMaker;

import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A worker that saves records to an underlying journal in the
 * background.
 */
@SuppressWarnings("deprecation")
@Log
class RecordSaveWorker implements Runnable {

    private static final String QUEUE_NAME = "queue";
    private final Journal journal;
    private final BlockingDeque<Record> pending;
    private final TxMaker diskDb;
    private final AtomicBoolean run = new AtomicBoolean(true);
    @Getter private int collectTime = 1000;
    @Getter private int batchMaxSize = 100;
    @Getter private int diskCommitSizeThreshold = 20;
    @Getter private int diskCommitTimeThreshold = 1000 * 10;

    RecordSaveWorker(Journal journal, BlockingDeque<Record> pending, TxMaker diskDb) {
        checkNotNull(journal, "journal");
        checkNotNull(pending, "queue");
        checkNotNull(diskDb, "diskDb");
        this.journal = journal;
        this.pending = pending;
        this.diskDb = diskDb;
    }

    public void shutdown() {
        if (!run.compareAndSet(true, false)) {
            log.warning("Tried to stop RecordSaveWorker more than once");
        }
    }

    private void commitPendingToDisk() {
        DB tx = diskDb.makeTx();
        BlockingQueue<Record> diskQueue = tx.getQueue(QUEUE_NAME);
        pending.drainTo(diskQueue);
        tx.commit();
    }

    private void commitToDisk(List<Record> records) {
        DB tx = diskDb.makeTx();
        BlockingQueue<Record> diskQueue = tx.getQueue(QUEUE_NAME);
        for (Record record : records) {
            diskQueue.add(record);
        }
        tx.commit();
    }

    @Override
    public void run() {
        List<Record> batch = Lists.newArrayList();

        // This method reduces the number of writes to disk (to zero) if
        // the the underlying DB is keeping up but it can also cause
        // a delay up to the DB connection / query timeout before
        // uncommitted data is written to disk in the event of DB failure

        while (run.get()) { // Repeatedly drain from the queue
            try {
                DB tx = diskDb.makeTx();
                BlockingQueue<Record> diskQueue = tx.getQueue(QUEUE_NAME);
                boolean fromPending = false;

                // Prefer to read entries from the disk queue before the pending queue
                diskQueue.drainTo(batch, batchMaxSize);

                // Unless the disk queue is empty:
                if (batch.isEmpty()) {
                    Queues.drain(pending, batch, batchMaxSize, collectTime, TimeUnit.MILLISECONDS);
                    fromPending = true;
                }

                // If we are behind (queue too large or entries too old), then save the pending changes to the disk queue
                if (!pending.isEmpty() && (pending.size() > diskCommitSizeThreshold
                        || pending.poll().getTime().getTime() < System.currentTimeMillis() - diskCommitTimeThreshold)) {
                    commitPendingToDisk();
                }

                if (!batch.isEmpty()) {
                    try {
                        journal.addRecords(batch);
                        tx.commit(); // Commit removal of items from the disk queue
                    } catch (Throwable e) {
                        if (fromPending) {
                            commitToDisk(batch); // Place entries in the disk queue
                        }
                        tx.rollback();
                        log.log(Level.WARNING, "Failed to add records to the journal; will re-attempt", e);
                    }

                    batch.clear();
                }
            } catch (InterruptedException ignored) {
            }

        }
    }

}
