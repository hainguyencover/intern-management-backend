package com.holaho.intern.reporting.service;

import com.holaho.intern.reporting.dto.ReportExportRequest;

public interface ReportExportService {

    byte[] exportReportDirect(ReportExportRequest request, Long currentUserId, String userEmail);
}
