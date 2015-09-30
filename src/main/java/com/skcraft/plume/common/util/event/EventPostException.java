package com.skcraft.plume.common.util.event;

public class EventPostException extends RuntimeException {

    public EventPostException() {
    }

    public EventPostException(String message) {
        super(message);
    }

    public EventPostException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventPostException(Throwable cause) {
        super(cause);
    }
}
