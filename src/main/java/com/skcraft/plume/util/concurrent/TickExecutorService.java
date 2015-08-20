package com.skcraft.plume.util.concurrent;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AbstractListeningExecutorService;
import com.google.inject.Singleton;
import com.skcraft.plume.common.util.module.AutoRegister;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import lombok.extern.java.Log;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkNotNull;

@AutoRegister
@Singleton
@Log
public class TickExecutorService extends AbstractListeningExecutorService {

    private final Queue<Runnable> tasks = new LinkedBlockingQueue<>();

    @Override
    public void shutdown() {
    }

    @Override
    public List<Runnable> shutdownNow() {
        return ImmutableList.of();
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void execute(Runnable command) {
        checkNotNull(command, "command");
        tasks.add(command);
    }

    @SubscribeEvent
    public void tickStart(TickEvent event) {
        Runnable task;
        while ((task = tasks.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to run task in tick", e);
            }
        }
    }
}
