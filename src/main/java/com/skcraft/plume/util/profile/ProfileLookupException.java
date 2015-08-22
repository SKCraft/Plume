package com.skcraft.plume.util.profile;

import lombok.Getter;

public class ProfileLookupException extends Exception {

    @Getter
    private final String name;

    public ProfileLookupException(String message, String name) {
        super(message);
        this.name = name;
    }

    public ProfileLookupException(String message, Throwable cause, String name) {
        super(message, cause);
        this.name = name;
    }

    public ProfileLookupException(Throwable cause, String name) {
        super(cause);
        this.name = name;
    }

}
