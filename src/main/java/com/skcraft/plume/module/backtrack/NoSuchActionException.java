package com.skcraft.plume.module.backtrack;

public class NoSuchActionException extends Exception {

    public NoSuchActionException() {
    }

    public NoSuchActionException(String message) {
        super(message);
    }

    public NoSuchActionException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchActionException(Throwable cause) {
        super(cause);
    }

}
