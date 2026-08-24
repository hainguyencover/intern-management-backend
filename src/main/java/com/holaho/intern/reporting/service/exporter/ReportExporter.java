package com.holaho.intern.reporting.service.exporter;

import com.holaho.intern.reporting.dto.ReportData;
import com.holaho.intern.reporting.enums.ExportFormat;

public interface ReportExporter {

    ExportFormat getFormat();

    byte[] export(ReportData data);
}
