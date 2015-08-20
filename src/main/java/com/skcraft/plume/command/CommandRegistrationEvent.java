package com.skcraft.plume.command;

import com.sk89q.intake.dispatcher.SimpleDispatcher;
import com.sk89q.intake.parametric.ParametricBuilder;
import lombok.Getter;

import static com.google.common.base.Preconditions.checkNotNull;

public class CommandRegistrationEvent {

    @Getter
    private final SimpleDispatcher dispatcher;
    @Getter
    private final ParametricBuilder builder;

    public CommandRegistrationEvent(SimpleDispatcher dispatcher, ParametricBuilder builder) {
        checkNotNull(dispatcher, "dispatcher");
        checkNotNull(builder, "builder");
        this.dispatcher = dispatcher;
        this.builder = builder;
    }

}
