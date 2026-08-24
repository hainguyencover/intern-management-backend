package com.holaho.intern.reporting.service;

import com.holaho.intern.reporting.dto.ReportCatalogDto;
import com.holaho.intern.reporting.dto.ReportFilterRequest;
import com.holaho.intern.reporting.dto.ReportPreviewResponse;

import java.util.List;

public interface ReportService {

    List<ReportCatalogDto> getReportCatalog();

    ReportPreviewResponse previewReport(String reportCode, ReportFilterRequest filter);
}
