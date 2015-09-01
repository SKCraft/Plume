package com.skcraft.plume.module.backtrack;

public class ActionReadException extends Exception {

    public ActionReadException() {
    }

    public ActionReadException(String message) {
        super(message);
    }

    public ActionReadException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActionReadException(Throwable cause) {
        super(cause);
    }
}
