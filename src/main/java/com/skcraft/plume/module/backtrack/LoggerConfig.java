package com.skcraft.plume.module.backtrack;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

public class LoggerConfig {

    @Setting(comment = "Configure how search works")
    public Search search = new Search();

    @Setting(comment = "Configure how the near command works")
    public Near near = new Near();

    @Setting(comment = "Configure the events that will be logged")
    public Events events = new Events();

    @Setting(comment = "Configure how rollback or replay works")
    public Playback playback = new Playback();

    @ConfigSerializable
    public static class Search {
        @Setting(comment = "The maximum number of results to return, by default")
        public int defaultLimit = 2000;

        @Setting(comment = "The maximum number of results that can be returned")
        public int maxLimit = 10000;
    }

    @ConfigSerializable
    public static class Near {
        @Setting(comment = "The default parameters for the near query")
        public String defaultParameters = "near 20 since 1w";
    }

    @ConfigSerializable
    public static class Playback {
        @Setting(comment = "The maximum amount of time (in ms) that rollback or replay may take up in a single tick (which itself is 20ms)")
        public int maxTimePerTick = 5;

        @Setting(comment = "The interval (in ms) between status updates on a rollback or replay")
        public int updateInterval = 5000;

        @Setting(comment = "Require a time or player to be specified to rollback or replay if -y is not specified in the command")
        public boolean confirmNoDateNoPlayer = true;
    }

    @ConfigSerializable
    public static class Events {
        @Setting public boolean blockBreak = true;
        @Setting public boolean blockPlace = true;
        @Setting public boolean bucketFill = true;
        @Setting public boolean entityDamage = true;
        @Setting public boolean explosion = true;
        @Setting public boolean itemDrop = true;
        @Setting public boolean itemPickup = true;
        @Setting public boolean playerChat = true;
        @Setting public boolean playerCommand = true;
        @Setting public boolean playerDeath = true;
    }

}
