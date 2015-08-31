package com.skcraft.plume.common.service.journal;

import lombok.Getter;
import lombok.extern.java.Log;
import org.mapdb.DBMaker;
import org.mapdb.TxMaker;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Buffers new records and only commits them to the underlying journal
 * if sufficient time passes or a threshold on the number of queue size
 * is reached.
 */
@Log
public class JournalBuffer {

    @Getter
    private final Journal journal;
    private final TxMaker dbTxMaker;
    private final BlockingDeque<Record> queue = new LinkedBlockingDeque<>();
    private final RecordSaveWorker worker;

    /**
     * Create a new instance.
     *
     * @param journal The underlying journal
     * @param file The file to store the queue, in case the server is shut down abruptly
     */
    @SuppressWarnings("deprecation")
    public JournalBuffer(Journal journal, File file) {
        checkNotNull(journal, "journal");
        checkNotNull(file, "file");

        this.journal = journal;

        dbTxMaker = DBMaker.fileDB(file)
                .fileChannelEnable()
                .asyncWriteEnable()
                .executorEnable()
                .closeOnJvmShutdown()
                .makeTxMaker();

        this.worker = new RecordSaveWorker(journal, queue, dbTxMaker);

        Thread thread = new Thread(worker);
        thread.setName("Plume Journal Buffer");
        thread.start();
    }

    /**
     * Shutdown the worker thread and commit and pending data.
     *
     * <p>The method may block.</p>
     */
    public void shutdown() {
        worker.shutdown();
        log.info("Shutting down journal buffer worker...");
        dbTxMaker.close();
    }

    /**
     * Queue a record for later insertion into the journal.
     *
     * @param record The record
     */
    public void addRecord(Record record) {
        checkNotNull(record, "entry");
        queue.add(record);
    }

    /**
     * Que a collection of records for later insertion into the journal.
     *
     * @param records A collection of records
     */
    public void addRecords(Collection<Record> records) {
        checkNotNull(records, "records");
        queue.addAll(records);
    }

}
