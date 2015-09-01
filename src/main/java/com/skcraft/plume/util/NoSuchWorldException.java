package com.skcraft.plume.util;

public class NoSuchWorldException extends Exception {

    public NoSuchWorldException() {
    }

    public NoSuchWorldException(String message) {
        super(message);
    }

    public NoSuchWorldException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchWorldException(Throwable cause) {
        super(cause);
    }

}
