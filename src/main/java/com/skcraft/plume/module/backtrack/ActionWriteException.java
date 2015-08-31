package com.skcraft.plume.module.backtrack;

public class ActionWriteException extends Exception {

    public ActionWriteException() {
    }

    public ActionWriteException(String message) {
        super(message);
    }

    public ActionWriteException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActionWriteException(Throwable cause) {
        super(cause);
    }
}
