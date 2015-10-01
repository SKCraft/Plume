package com.skcraft.plume.module.perf.watchdog;

import com.google.common.io.CharSource;
import com.skcraft.plume.common.event.ReportGenerationEvent;
import com.skcraft.plume.common.util.event.EventBus;
import com.skcraft.plume.util.management.ThreadMonitor;
import cpw.mods.fml.common.FMLCommonHandler;
import lombok.extern.java.Log;
import net.minecraft.server.MinecraftServer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;

@Log
enum Action {

    THREAD_DUMP {
        @Override
        public void execute(EventBus eventBus, Thread serverThread) {
            log.info("Writing a thread dump because the server has been frozen for some time now");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ThreadMonitor monitor = new ThreadMonitor();
            try (PrintStream printStream = new PrintStream(baos)) {
                monitor.threadDump(printStream);
            }
            try {
                String report = baos.toString("UTF-8");
                ReportGenerationEvent event = new ReportGenerationEvent("watchdog-freeze", "txt", CharSource.wrap(report));
                eventBus.post(event);
            } catch (UnsupportedEncodingException e) {
                log.log(Level.WARNING, "Failed to write freeze report for Watchdog", e);
            }
        }
    },
    GRACEFUL_SHUTDOWN {
        @SuppressWarnings("deprecation")
        @Override
        public void execute(EventBus eventBus, Thread serverThread) {
            log.log(Level.WARNING, "Starting graceful server shutdown as the server thread has frozen...");
            MinecraftServer.getServer().initiateShutdown();
            serverThread.stop(); // Fire ThreadDeath event
            serverThread.interrupt(); // Sets interruption flag
            // These steps won't necessarily stop the server
        }
    },
    TERMINATE_SERVER {
        @SuppressWarnings("deprecation")
        @Override
        public void execute(EventBus eventBus, Thread serverThread) {
            log.log(Level.WARNING, "Forcing server shutdown as the server thread has frozen...");
            FMLCommonHandler.instance().exitJava(1, true);
        }
    };

    public abstract void execute(EventBus eventBus, Thread serverThread);

}
