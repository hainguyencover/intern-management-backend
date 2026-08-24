package com.holaho.intern.evaluation.export;

import com.holaho.intern.evaluation.dto.FinalReportDetailResponse;

public interface ReportExporter {
    byte[] export(FinalReportDetailResponse report);
    String getContentType();
    String getFileExtension();
}
