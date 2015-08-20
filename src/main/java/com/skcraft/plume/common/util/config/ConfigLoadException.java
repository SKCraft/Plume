package com.skcraft.plume.common.util.config;

public class ConfigLoadException extends RuntimeException {

    public ConfigLoadException() {
    }

    public ConfigLoadException(String message) {
        super(message);
    }

    public ConfigLoadException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigLoadException(Throwable cause) {
        super(cause);
    }

}
