package com.skcraft.plume.module.perf;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

class SensibleNumbersConfig {

    @Setting(comment = "Configure the creature quotas used in the vanilla creature spawner")
    public CreatureQuotas quotas = new CreatureQuotas();

    @Setting(comment = "Limit mob spawning based on the number of existing mobs in the nearby area")
    public RadiusHardLimit radiusHardLimit = new RadiusHardLimit();

    @ConfigSerializable
    public static class CreatureQuotas {
        @Setting
        public int monster = 70;
        @Setting
        public int animal = 10;
        @Setting
        public int ambient = 15;
        @Setting
        public int water = 5;
    }

    @ConfigSerializable
    public static class RadiusHardLimit {
        @Setting(comment = "Enable to attempt to block spawning entities based on radius")
        public boolean enabled = false;

        @Setting
        public CountPerRadius zombie = new CountPerRadius(2, 5);
        @Setting
        public CountPerRadius skeleton = new CountPerRadius(2, 5);
        @Setting
        public CountPerRadius creeper = new CountPerRadius(2, 5);
        @Setting
        public CountPerRadius spider = new CountPerRadius(2, 5);
        @Setting
        public CountPerRadius enderman = new CountPerRadius(2, 5);
        @Setting
        public CountPerRadius witch = new CountPerRadius(2, 5);
        @Setting
        public CountPerRadius squid = new CountPerRadius(1, 5);
        @Setting
        public CountPerRadius sheep = new CountPerRadius(5, 5);
        @Setting
        public CountPerRadius pig = new CountPerRadius(5, 5);
        @Setting
        public CountPerRadius cow = new CountPerRadius(5, 5);
        @Setting
        public CountPerRadius chicken = new CountPerRadius(5, 5);
        @Setting
        public CountPerRadius pet = new CountPerRadius(5, 5);
    }

    @ConfigSerializable
    public static class CountPerRadius {
        @Setting
        public int limit = 10;
        @Setting
        public int radius = 1;

        public CountPerRadius() {
        }

        public CountPerRadius(int limit, int radius) {
            this.limit = limit;
            this.radius = radius;
        }
    }

}
