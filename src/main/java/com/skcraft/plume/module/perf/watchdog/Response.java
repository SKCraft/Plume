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

    @Setting(comment = "In seconds")
    private long time = 30;
    @Setting(comment = "Choose between THREAD_DUMP, GRACEFUL_SHUTDOWN and TERMINATE_SERVER")
    private Action action = Action.THREAD_DUMP;

    @Override
    public int compareTo(Response o) {
        if (time < o.time) {
            return -1;
        } else if (time > o.time) {
            return 1;
        } else {
            return 0;
        }
    }

}
