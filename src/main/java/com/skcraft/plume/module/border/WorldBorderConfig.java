package com.skcraft.plume.module.border;

import com.skcraft.plume.util.Location3d;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

public class WorldBorderConfig {

    @Setting("border")
    public Border border = new Border();

    @ConfigSerializable
    public static class Border {

        @Setting(comment = "The radius of the border, in blocks, from the spawn point")
        public int borderSize = 10000;

        @Setting(comment = "The radius of the buffer zone, in blocks, from the spawn point")
        public int bufferSize = 9990;

        @Setting(comment = "The radius of the snapback zone, in blocks, from the spawn point")
        public int snapBackSize = 9970;

        @Setting(comment = "The type of border, can currently only be CIRCLE")
        public BorderType borderType = BorderType.CIRCLE;

        public enum BorderType {
            CIRCLE {
                @Override
                public Threshold getThreshold(Location3d current, Location3d center, int radius, int buffer) {
                    int distance = (int) (Math.pow(center.getX() - current.getX(), 2) + Math.pow(center.getZ() - current.getZ(), 2));

                    if (distance > radius * radius) {
                        return Threshold.ESCAPED;
                    } else if (distance > buffer * buffer) {
                        return Threshold.BUFFER;
                    } else {
                        return Threshold.CLEAR;
                    }
                }

                @Override
                public Location3d getSnapBackLocation(Location3d current, Location3d center, int radius) {
                    return current.subtract(center).unit().multiply(radius).add(center);
                }
            };

            public abstract Threshold getThreshold(Location3d current, Location3d centre, int radius, int buffer);

            public abstract Location3d getSnapBackLocation(Location3d current, Location3d center, int radius);
        }

        public enum Threshold {
            BUFFER,
            CLEAR,
            ESCAPED
        }
    }
}
