package com.skcraft.plume.module.perf.watchdog;

import lombok.Getter;
import lombok.Setter;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
@Getter
@Setter
class Response implements Comparable<Response> {

    @Setting(comment = "When this rule is triggered, defined in seconds")
    private long threshold = 30;
    @Setting(comment = "BROADCAST, THREAD_DUMP, INTERRUPT_TICKING (not recommended), GRACEFUL_SHUTDOWN, or TERMINATE_SERVER")
    private Action action = Action.THREAD_DUMP;
    @Setting(comment = "Used if the action is BROADCAST")
    private String message = "\u00a72Uh oh! The server is currently frozen (%d seconds).";

    public Response() {
    }

    public Response(long threshold, Action action) {
        this.threshold = threshold;
        this.action = action;
    }

    public Response(long threshold, Action action, String message) {
        this.threshold = threshold;
        this.action = action;
        this.message = message;
    }

    @Override
    public int compareTo(Response o) {
        if (threshold < o.threshold) {
            return -1;
        } else if (threshold > o.threshold) {
            return 1;
        } else {
            return 0;
        }
    }

}
