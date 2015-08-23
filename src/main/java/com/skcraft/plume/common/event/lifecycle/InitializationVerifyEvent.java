package com.skcraft.plume.common.event.lifecycle;

import com.google.common.collect.Lists;
import com.skcraft.plume.common.util.FatalError;
import lombok.Getter;

import java.util.List;

public class InitializationVerifyEvent {

    @Getter
    private final List<FatalError> fatalErrors = Lists.newArrayList();

}
