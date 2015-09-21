package com.skcraft.plume.module.perf.profiler;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Lists;
import lombok.extern.java.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

@Log
public class CSVReportBuilder implements Callable<File> {

    private final Collection<Timing> timings;
    private final List<Appender> appenders = Lists.newArrayList(new TimingAppender());
    private final File file;

    public CSVReportBuilder(Collection<Timing> timings, List<Appender> appenders, File file) {
        this.timings = timings;
        this.appenders.addAll(appenders);
        this.file = file;
    }

    @Override
    public File call() throws Exception {
        try (FileWriter fw = new FileWriter(file);
             BufferedWriter bw = new BufferedWriter(fw);
             CSVWriter csv = new CSVWriter(bw)) {

            List<String> columns = Lists.newArrayList();
            for (Appender appender : appenders) {
                columns.addAll(appender.getColumns());
            }
            csv.writeNext(columns.toArray(new String[columns.size()]));

            for (Timing timing : timings) {
                List<String> values = Lists.newArrayList();
                for (Appender appender : appenders) {
                    values.addAll(appender.getValues(timing));
                }
                csv.writeNext(values.toArray(new String[values.size()]));
            }

            return file;
        }
    }
}
