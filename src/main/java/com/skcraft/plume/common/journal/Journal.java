package com.skcraft.plume.common.journal;

import com.skcraft.plume.common.util.Cursor;
import com.skcraft.plume.common.util.Order;

import java.util.Collection;

/**
 * Manages a list of changes, some of which may be reversible.
 */
public interface Journal {

    /**
     * Fetch a list of records matching the given criteria.
     *
     * <p>The returned cursor must be closed.</p>
     *
     * @param criteria The criteria to match
     * @param order The order by which the records should be sorted in regards to time
     * @return A cursor that provides access to the data
     */
    Cursor<Record> queryRecords(Criteria criteria, Order order);

    /**
     * Add the given records to the journal.
     *
     * <p>This method will block.</p>
     *
     * @param records The records
     */
    void addRecords(Collection<Record> records);

}
