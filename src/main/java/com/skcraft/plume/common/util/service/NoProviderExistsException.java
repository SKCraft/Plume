package com.skcraft.plume.common.util.service;

public class NoProviderExistsException extends RuntimeException {

    public NoProviderExistsException() {
    }

    public NoProviderExistsException(String message) {
        super(message);
    }

    public NoProviderExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoProviderExistsException(Throwable cause) {
        super(cause);
    }
}
