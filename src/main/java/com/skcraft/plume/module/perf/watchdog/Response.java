package com.skcraft.plume.module.perf.watchdog;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
class Response implements Comparable<Response> {

    @Setting(comment = "When this rule is triggered, defined in seconds")
    private long threshold = 30;
    @Setting(comment = "Choose THREAD_DUMP, INTERRUPT_TICKING (strongly not recommended), or TERMINATE_SERVER")
    private Action action = Action.THREAD_DUMP;

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
