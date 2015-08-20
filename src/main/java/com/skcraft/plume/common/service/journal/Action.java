package com.skcraft.plume.common.service.journal;

/**
 * An action represents a recordable change.
 */
public interface Action {

    /**
     * Parse data that was previously created by {@link #writeData()}.
     *
     * @param data The data to parse
     */
    void readData(String data);

    /**
     * Write out the data necessary to later recreate this action.
     *
     * @return The data to persist
     */
    String writeData();

    /**
     * Revert the change.
     */
    void revert();

    /**
     * Apply the change.
     */
    void apply();

}
