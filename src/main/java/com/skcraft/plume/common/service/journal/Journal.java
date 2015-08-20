package com.skcraft.plume.common.service.journal;

import com.skcraft.plume.common.DataAccessException;
import com.skcraft.plume.common.util.Order;

import java.util.Collection;
import java.util.List;

/**
 * Manages a list of changes, some of which may be reversible.
 */
public interface Journal {

    /**
     * Load data necessary to use the object. Calling this again refreshes
     * the data.
     *
     * @throws DataAccessException Thrown if data can't be accessed
     */
    default void load() {}

    /**
     * Fetch a list of records matching the given criteria.
     *
     * @param criteria The criteria to match
     * @param order The order by which the records should be sorted in regards to time
     * @param limit The maximum number of entries to return
     * @return A list of records
     */
    List<Record> queryRecords(Criteria criteria, Order order, int limit);

    /**
     * Add the given records to the journal.
     *
     * <p>This method will block.</p>
     *
     * @param records The records
     */
    void addRecords(Collection<Record> records);

}
