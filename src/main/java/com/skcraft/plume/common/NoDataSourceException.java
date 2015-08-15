package com.skcraft.plume.common;

public class NoDataSourceException extends Exception {

    public NoDataSourceException() {
    }

    public NoDataSourceException(String message) {
        super(message);
    }

    public NoDataSourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoDataSourceException(Throwable cause) {
        super(cause);
    }

}
