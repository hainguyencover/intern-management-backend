package com.holaho.intern.reporting.service;

import com.holaho.intern.reporting.dto.ExportJobResponse;
import com.holaho.intern.reporting.dto.ReportExportRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExportJobService {

    ExportJobResponse createExportJob(ReportExportRequest request, Long requestedBy, String userEmail);

    ExportJobResponse getJobStatus(String jobCode);

    byte[] downloadJobFile(String jobCode, Long currentUserId);

    Page<ExportJobResponse> getExportHistory(Pageable pageable);

    void processJobAsync(Long jobId, Long tenantId, String userEmail);

    void cleanupExpiredJobs();
}
