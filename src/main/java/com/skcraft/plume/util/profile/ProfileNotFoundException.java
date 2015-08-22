package com.skcraft.plume.util.profile;

import lombok.Getter;

import static com.google.common.base.Preconditions.checkNotNull;

public class ProfileNotFoundException extends Exception {

    @Getter
    private final String name;

    public ProfileNotFoundException(String name) {
        checkNotNull(name, "name");
        this.name = name;
    }

}
