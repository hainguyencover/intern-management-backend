package com.holaho.intern.reporting.service.generator;

import com.holaho.intern.reporting.dto.ReportData;
import com.holaho.intern.reporting.dto.ReportFilterRequest;

import java.util.List;

public interface ReportGenerator {

    String getReportCode();

    String getReportName();

    String getDescription();

    String getCategory();

    List<String> getAvailableFilters();

    ReportData generate(ReportFilterRequest filter, Long tenantId);
}
