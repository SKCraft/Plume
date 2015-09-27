package com.skcraft.plume.common.event;

import com.google.common.collect.Lists;
import com.google.common.io.CharSource;
import lombok.Getter;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Getter
public class ReportGenerationEvent {

    private final String name;
    private final String extension;
    private final CharSource reportSource;
    private final List<String> messages = Lists.newArrayList();

    public ReportGenerationEvent(String name, String extension, CharSource reportSource) {
        checkNotNull(name, "name");
        checkNotNull(extension, "extension");
        checkNotNull(reportSource, "reportSource");
        this.name = name;
        this.extension = extension;
        this.reportSource = reportSource;
    }

}
