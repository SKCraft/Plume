package com.skcraft.plume.module.exporter;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.io.CharSource;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.event.ReportGenerationEvent;
import com.skcraft.plume.common.util.concurrent.Deferreds;
import com.skcraft.plume.common.util.event.EventBus;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.concurrent.TickExecutorService;
import net.minecraft.command.ICommandSender;

import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.Executors;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "exporter", desc = "Commands to dump all entities, tile entities, and so on to CSV files")
public class Exporter {

    private final Map<String, Class<? extends CSVExporter>> exporters = Maps.newHashMap();
    private final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
    @Inject private TickExecutorService tickExecutor;
    @Inject private EventBus eventBus;

    public Exporter() {
        exporters.put("chunks", ChunkExporter.class);
        exporters.put("entities", EntityExporter.class);
        exporters.put("tile-entities", TileEntityExporter.class);
        exporters.put("chunk-tickets", ChunkTicketExporter.class);
    }

    @Command(aliases = "exportdata", desc = "Exports data from the server")
    @Require("plume.exporter")
    public void dump(@Sender ICommandSender sender, String type) throws Exception {
        Class<? extends CSVExporter> exporterClass = exporters.get(type);
        if (exporterClass != null) {
            Deferreds
                    .when(() -> {
                        CSVExporter exporter = exporterClass.newInstance();
                        sender.addChatMessage(Messages.subtle(tr("exporter.collectingData")));
                        exporter.collectData();
                        return exporter;
                    }, tickExecutor)
                    .filter(exporter -> {
                        StringWriter writer = new StringWriter();

                        try (CSVWriter csv = new CSVWriter(writer)) {
                            exporter.writeData(csv);
                        }

                        ReportGenerationEvent event = new ReportGenerationEvent(type, "csv", CharSource.wrap(writer.toString()));
                        eventBus.post(event);
                        return event.getMessages();
                    }, executorService)
                    .done(messages -> {
                        for (String message : messages) {
                            // TODO: Sender may no longer work by this time
                            sender.addChatMessage(Messages.info(message));
                        }
                    }, tickExecutor)
                    .fail(e -> {
                        sender.addChatMessage(Messages.exception(e));
                    }, tickExecutor);
        } else {
            sender.addChatMessage(Messages.error(tr("exporter.unknownType", Joiner.on(", ").join(exporters.keySet()))));
        }
    }

}
