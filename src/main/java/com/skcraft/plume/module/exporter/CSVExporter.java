package com.skcraft.plume.module.exporter;

import au.com.bytecode.opencsv.CSVWriter;

import java.io.IOException;

public interface CSVExporter {

    void collectData() throws Exception;

    void writeData(CSVWriter writer) throws IOException;

}
