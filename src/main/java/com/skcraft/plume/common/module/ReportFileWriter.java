package com.skcraft.plume.common.module;

import com.google.common.io.CharStreams;
import com.skcraft.plume.common.event.ReportGenerationEvent;
import com.skcraft.plume.common.util.PathnameBuilder;
import com.skcraft.plume.common.util.StringInterpolation;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.event.Subscribe;
import com.skcraft.plume.common.util.module.Module;
import lombok.extern.java.Log;
import ninja.leaping.configurate.objectmapping.Setting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "report-file-writer", desc = "Writes generated reports (from other modules, like profilers) to a folder")
@Log
public class ReportFileWriter {

    @InjectConfig("report_file_writer") private Config<WriterConfig> config;

    private File getReportPath(String name, String extension) {
        PathnameBuilder builder = new PathnameBuilder();
        String path = config.get().reportPath;
        path = builder.interpolate(path);
        path = StringInterpolation.interpolate(StringInterpolation.BRACE_PATTERN, path, input -> {
            switch (input) {
                case "name":
                    return name;
                case "extension":
                case "ext":
                    return extension;
                default:
                    return null;
            }
        });
        return new File(path);
    }

    @Subscribe
    public void onReportGenerationEvent(ReportGenerationEvent event) {
        File file = getReportPath(event.getName(), event.getExtension());
        file.getAbsoluteFile().getParentFile().mkdirs();

        try (FileWriter fw = new FileWriter(file);
             BufferedWriter bw = new BufferedWriter(fw);
             BufferedReader br = event.getReportSource().openBufferedStream()) {
            CharStreams.copy(br, bw);
            event.getMessages().add(tr("reportFileWriter.wroteReport", file.getAbsolutePath()));
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to write report to " + file.getAbsolutePath(), e);
            event.getMessages().add(tr("reportFileWriter.writeFailed", file.getAbsolutePath()));
        }
    }

    private static class WriterConfig {
        @Setting(comment = "The path where the reports are written")
        private String reportPath = "reports/{name}_%Y-%m-%d-%H-%i-%s.{ext}";
    }

}
