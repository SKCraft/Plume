package com.skcraft.plume.module.perf.watchdog;

import com.google.common.io.CharSource;
import com.skcraft.plume.common.event.ReportGenerationEvent;
import com.skcraft.plume.common.util.event.EventBus;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.management.ThreadMonitor;
import cpw.mods.fml.common.FMLCommonHandler;
import lombok.extern.java.Log;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;

@Log
enum Action {

    THREAD_DUMP {
        @Override
        public void execute(Watchdog watchdog, EventBus eventBus, Thread serverThread, long stallTime) {
            log.info("[STALL FOR " + stallTime + " SECOND(S)] Executing THREAD_DUMP action...");
            writeThreadDump(watchdog, eventBus, "watchdog-stall");
        }
    },
    INTERRUPT_TICKING {
        @SuppressWarnings("deprecation")
        @Override
        public void execute(Watchdog watchdog, EventBus eventBus, Thread serverThread, long stallTime) {
            synchronized (watchdog.getThreadInterruptLock()) {
                if (watchdog.getCurrentTickingObject() != null) {
                    log.info("[STALL FOR " + stallTime + " SECOND(S)] Executing INTERRUPT_TICKING action to interrupt the current running tile entity or entity...");
                    watchdog.setCurrentTickingObject(false);
                    watchdog.setCatchingTickInterrupt(true);
                    serverThread.stop();
                } else {
                    log.info("[STALL FOR " + stallTime + " SECOND(S)] Trying to execute INTERRUPT_TICKING but the stall doesn't seem to be caused by a tile entity or entity");
                }
            }
        }
    },
    TERMINATE_SERVER {
        @Override
        public void execute(Watchdog watchdog, EventBus eventBus, Thread serverThread, long stallTime) {
            log.info("[STALL FOR " + stallTime + " SECOND(S)] Executing TERMINATE_SERVER action... the world will not be saved! A thread dump will also be created.");
            writeThreadDump(watchdog, eventBus, "watchdog-terminate");
            FMLCommonHandler.instance().exitJava(1, true);
        }
    };

    public abstract void execute(Watchdog watchdog, EventBus eventBus, Thread serverThread, long stallTime);

    private static void writeThreadDump(Watchdog watchdog, EventBus eventBus, String name) {
        Object currentTickingObject = watchdog.getCurrentTickingObject();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ThreadMonitor monitor = new ThreadMonitor();
        try (PrintStream printStream = new PrintStream(baos)) {
            if (currentTickingObject instanceof TileEntity) {
                printStream.println("Ticking tile entity: " + Messages.toString((TileEntity) currentTickingObject));
                printStream.println();
            } else if (currentTickingObject instanceof Entity) {
                printStream.println("Ticking entity: " + Messages.toString((Entity) currentTickingObject));
                printStream.println();
            }

            monitor.threadDump(printStream);
        }
        try {
            String report = baos.toString("UTF-8");
            ReportGenerationEvent event = new ReportGenerationEvent(name, "txt", CharSource.wrap(report));
            eventBus.post(event);
        } catch (UnsupportedEncodingException e) {
            log.log(Level.WARNING, "Failed to write freeze report for Watchdog", e);
        }
    }

}
