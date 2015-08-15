package com.skcraft.plume.event;

/**
 * A bulk event contains several affected objects in a list.
 */
public interface BulkEvent {

    /**
     * Get the actual result.
     *
     * <p>By default, bulk events will set the result to DENY if the number of
     * affected objects drops to zero. This method returns the true result.</p>
     *
     * @return The explicit result
     */
    Result getExplicitResult();

}
