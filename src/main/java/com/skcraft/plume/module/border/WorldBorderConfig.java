package com.skcraft.plume.module.border;

import com.skcraft.plume.util.Location3d;
import net.minecraft.world.World;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

public class WorldBorderConfig {

    @Setting("border")
    public Border border = new Border();

    @ConfigSerializable
    public static class Location {
        @Setting
        public int x;

        @Setting
        public int y;

        @Setting
        public int z;

        @Setting
        public World world;

        public Location3d getLocation() {
            return new Location3d(world, x, y, z);
        }
    }

    @ConfigSerializable
    public static class Border {

        @Setting(comment = "The radius of the border, in blocks, from the spawnpoint.")
        public int borderSize = 50;

        @Setting(comment = "The radius of the buffer zone, in blocks, from the spawnpoint.")
        public int bufferSize = 40;

        @Setting(comment = "The type of border, can either be SQUARE or CIRCLE")
        public BorderType borderType = BorderType.SQUARE;

        @Setting(comment = "The centre location for the border.")
        public Location centre = new Location();

        public enum BorderType {
            SQUARE,
            CIRCLE;

            public Threshold getThreshold(Location3d current, Location3d centre, int radius, int buffer) {
                if (this == BorderType.CIRCLE) {
                    double distance = (
                            Math.pow(current.getX() - current.getZ(), 2)
                            + Math.pow(centre.getX() - centre.getZ(), 2));

                    if (distance == Math.pow(radius, 2))
                        return Threshold.BORDER;
                    if (distance >= Math.pow(buffer, 2))
                        return Threshold.BUFFER;

                    if (distance < 0) {
                        if (distance == Math.pow(radius, 2) * -1)
                            return Threshold.BORDER;
                        if (distance <= Math.pow(buffer, 2) * -1)
                            return Threshold.BUFFER;
                    }

                    if (distance > Math.pow(radius, 2) || distance < Math.pow(radius, 2) * -1)
                        return Threshold.ESCAPED;

                    return Threshold.CLEAR;
                } else if (this == BorderType.SQUARE) {
                    if (current.getX() > centre.getX()) {
                        if (current.getX() == centre.getX() + radius)
                            return Threshold.BORDER;
                        if (current.getX() >= centre.getX() + buffer)
                            return Threshold.BUFFER;
                    } else {
                        if (current.getX() == centre.getX() - radius)
                            return Threshold.BORDER;
                        if (current.getX() <= centre.getX() - buffer)
                            return Threshold.BUFFER;
                    }

                    if (current.getZ() > centre.getZ()) {
                        if (current.getZ() == centre.getZ() + radius)
                            return Threshold.BORDER;
                        if (current.getZ() >= centre.getZ() + buffer)
                            return Threshold.BUFFER;
                    } else {
                        if (current.getZ() == centre.getZ() - radius)
                            return Threshold.BORDER;
                        if (current.getZ() <= centre.getZ() - buffer)
                            return Threshold.BUFFER;
                    }

                    if (current.getX() > centre.getX() + radius ||
                            current.getX() < centre.getX() - radius ||
                            current.getZ() > centre.getZ() + radius ||
                            current.getZ() < centre.getZ() - radius)
                        return Threshold.ESCAPED;

                    return Threshold.CLEAR;
                } else return null;
            }
        }

        public enum Threshold {
            BORDER,
            BUFFER,
            CLEAR,
            ESCAPED
        }
    }
}
