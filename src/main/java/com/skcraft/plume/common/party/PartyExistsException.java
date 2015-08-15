package com.skcraft.plume.common.party;

/**
 * Thrown when a party exists already.
 */
public class PartyExistsException extends Exception {

    public PartyExistsException() {
    }

    public PartyExistsException(String message) {
        super(message);
    }

    public PartyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public PartyExistsException(Throwable cause) {
        super(cause);
    }

}
