package com.skcraft.plume.service;

import com.google.common.collect.Maps;
import com.skcraft.plume.common.util.collect.ReverseEvictingQueue;
import com.skcraft.plume.common.util.module.AutoRegister;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@AutoRegister
public class ClockHistory {

    private static final long SNAPSHOT_INTERVAL = TimeUnit.SECONDS.toNanos(1);
    private static final int HISTORY_LENGTH = 300;

    private final ReverseEvictingQueue<Double> tickTimes = ReverseEvictingQueue.create(HISTORY_LENGTH);
    private final ReverseEvictingQueue<Map<Integer, Double>> worldTickTimes = ReverseEvictingQueue.create(HISTORY_LENGTH);
    private long lastSnapshot = 0;

    @SubscribeEvent
    public void onTick(ServerTickEvent event) {
        long now = System.nanoTime();
        if (now - lastSnapshot >= SNAPSHOT_INTERVAL) {
            MinecraftServer server = MinecraftServer.getServer();

            double averageTime = MathHelper.average(server.tickTimeArray) * 1.0E-6D;

            Map<Integer, Double> averageWorldTimes = Maps.newHashMap();
            for (Map.Entry<Integer, long[]> entry : server.worldTickTimes.entrySet()) {
                averageWorldTimes.put(entry.getKey(), MathHelper.average(entry.getValue()) * 1.0E-6D);
            }

            synchronized (this) {
                tickTimes.add(averageTime);
                worldTickTimes.add(averageWorldTimes);
            }

            lastSnapshot = now;
        }
    }

    public Double[] getAverageTickTimes(long... durations) {
        checkNotNull(durations, "durations");
        checkArgument(durations.length > 0, "durations.length > 0");

        Double[] results = new Double[durations.length];
        int index = 0;

        synchronized (this) {
            double sum = 0;
            int count = 0;

            loopTimes:
            for (double value : tickTimes) {
                sum += value;
                count++;

                while (count >= durations[index]) {
                    results[index] = sum / count;
                    index++;
                    if (index >= durations.length) {
                        break loopTimes;
                    }
                }
            }
        }

        return results;
    }

    public Map<String, Double>[] getAverageWorldTickTimes(long... durations) {
        checkNotNull(durations, "durations");
        checkArgument(durations.length > 0, "durations.length > 0");

        @SuppressWarnings("unchecked")
        Map<String, Double>[] results = (Map<String, Double>[]) Array.newInstance(Map.class, durations.length);
        int index = 0;

        TIntObjectMap<Summer> summers = new TIntObjectHashMap<>();
        synchronized (this) {
            int count = 0;

            loopTimes:
            for (Map<Integer, Double> map : worldTickTimes) {
                for (Map.Entry<Integer, Double> entry : map.entrySet()) {
                    Summer summer = summers.get(entry.getKey());
                    if (summer == null) {
                        summers.put(entry.getKey(), new Summer(entry.getValue()));
                    } else {
                        summer.add(entry.getValue());
                    }

                    count++;

                    while (count >= durations[index]) {
                        Map<String, Double> averages = Maps.newHashMap();
                        summers.forEachEntry((a, b) -> {
                            averages.put(String.valueOf(a), b.average());
                            return true;
                        });
                        results[index] = averages;

                        index++;
                        if (index >= durations.length) {
                            break loopTimes;
                        }
                    }
                }
            }
        }

        return results;
    }

    public static double toTickRate(double tickTime) {
        return Math.min(20.0, 1000 / tickTime);
    }

    private static class Summer {
        private int count = 1;
        private double sum;

        public Summer(double sum) {
            this.sum = sum;
        }

        public void add(double value) {
            sum += value;
            count++;
        }

        public double average() {
            return sum / count;
        }
    }

}
