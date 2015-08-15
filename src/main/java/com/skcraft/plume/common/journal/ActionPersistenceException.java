package com.skcraft.plume.common.journal;

/**
 * Thrown if an action could not be persisted or parsed.
 */
public class ActionPersistenceException extends RuntimeException {

    public ActionPersistenceException() {
    }

    public ActionPersistenceException(String message) {
        super(message);
    }

    public ActionPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActionPersistenceException(Throwable cause) {
        super(cause);
    }

}
