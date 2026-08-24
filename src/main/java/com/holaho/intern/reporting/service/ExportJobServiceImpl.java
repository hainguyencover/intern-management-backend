package com.holaho.intern.reporting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.holaho.intern.reporting.dto.ExportJobResponse;
import com.holaho.intern.reporting.dto.ReportData;
import com.holaho.intern.reporting.dto.ReportExportRequest;
import com.holaho.intern.reporting.dto.ReportFilterRequest;
import com.holaho.intern.reporting.entity.ExportJob;
import com.holaho.intern.reporting.enums.ExportFormat;
import com.holaho.intern.reporting.enums.ExportJobStatus;
import com.holaho.intern.reporting.repository.ExportJobRepository;
import com.holaho.intern.reporting.service.exporter.ExcelReportExporter;
import com.holaho.intern.reporting.service.exporter.PdfReportExporter;
import com.holaho.intern.reporting.service.generator.ReportGenerator;
import com.holaho.intern.reporting.service.generator.ReportGeneratorRegistry;
import com.holaho.intern.reporting.service.storage.ReportStorageService;
import com.holaho.intern.service.AuditLogService;
import com.holaho.intern.shared.config.TenantContext;
import com.holaho.intern.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportJobServiceImpl implements ExportJobService {

    private final ExportJobRepository exportJobRepository;
    private final ReportGeneratorRegistry generatorRegistry;
    private final ExcelReportExporter excelReportExporter;
    private final PdfReportExporter pdfReportExporter;
    private final ReportStorageService reportStorageService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ExportJobResponse createExportJob(ReportExportRequest request, Long requestedBy, String userEmail) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) tenantId = 1L;

        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String shortUuid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String jobCode = "EXP-" + datePrefix + "-" + shortUuid;

        String filterJson = null;
        try {
            if (request.getFilters() != null) {
                filterJson = objectMapper.writeValueAsString(request.getFilters());
            }
        } catch (Exception e) {
            log.warn("Failed to serialize report filters to JSON: {}", e.getMessage());
        }

        ExportJob job = ExportJob.builder()
            .jobCode(jobCode)
            .reportCode(request.getReportCode())
            .format(request.getFormat())
            .status(ExportJobStatus.QUEUED)
            .filters(filterJson)
            .requestedBy(requestedBy != null ? requestedBy : 1L)
            .build();
        job.setTenantId(tenantId);

        ExportJob savedJob = exportJobRepository.save(job);

        // Trigger Async Execution
        this.processJobAsync(savedJob.getId(), tenantId, userEmail);

        return mapToResponse(savedJob);
    }

    @Async
    @Override
    @Transactional
    public void processJobAsync(Long jobId, Long tenantId, String userEmail) {
        TenantContext.setCurrentTenantId(tenantId);
        try {
            ExportJob job = exportJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("ExportJob not found with ID: " + jobId));

            job.setStatus(ExportJobStatus.PROCESSING);
            job.setStartedAt(LocalDateTime.now());
            exportJobRepository.save(job);

            ReportGenerator generator = generatorRegistry.get(job.getReportCode());
            ReportFilterRequest filter = new ReportFilterRequest();
            if (job.getFilters() != null) {
                try {
                    filter = objectMapper.readValue(job.getFilters(), ReportFilterRequest.class);
                } catch (Exception e) {
                    log.warn("Failed to deserialize filters JSON: {}", e.getMessage());
                }
            }

            ReportData data = generator.generate(filter, tenantId);
            if (userEmail != null) {
                data.setGeneratedBy(userEmail);
            }

            byte[] content;
            if (job.getFormat() == ExportFormat.XLSX) {
                content = excelReportExporter.export(data);
            } else {
                content = pdfReportExporter.export(data);
            }

            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = job.getReportCode().toLowerCase() + "_" + dateStr + job.getFormat().getFileExtension();

            String filePath = reportStorageService.storeFile(fileName, content);

            job.setStatus(ExportJobStatus.READY);
            job.setFileName(fileName);
            job.setFilePath(filePath);
            job.setFileSize((long) content.length);
            job.setRecordCount(data.getRecordCount());
            job.setCompletedAt(LocalDateTime.now());
            job.setExpiresAt(LocalDateTime.now().plusDays(1)); // File valid for 24 hours
            exportJobRepository.save(job);

            log.info("ExportJob {} completed successfully with {} records", job.getJobCode(), data.getRecordCount());

            // Audit log
            auditLogService.createAuditLog(
                job.getRequestedBy(),
                userEmail != null ? userEmail : "system",
                "REPORT_EXPORTED",
                "REPORT_JOB",
                job.getId(),
                "SUCCESS",
                "Job xuất báo cáo " + job.getJobCode() + " hoàn thành (" + data.getRecordCount() + " bản ghi)",
                null,
                null,
                null,
                null
            );

        } catch (Exception e) {
            log.error("Failed to process ExportJob ID {}: {}", jobId, e.getMessage(), e);
            exportJobRepository.findById(jobId).ifPresent(job -> {
                job.setStatus(ExportJobStatus.FAILED);
                job.setErrorMessage(e.getMessage());
                job.setCompletedAt(LocalDateTime.now());
                exportJobRepository.save(job);
            });
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ExportJobResponse getJobStatus(String jobCode) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) tenantId = 1L;

        ExportJob job = exportJobRepository.findByTenantIdAndJobCode(tenantId, jobCode)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Yêu cầu xuất báo cáo với mã: " + jobCode));

        return mapToResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadJobFile(String jobCode, Long currentUserId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) tenantId = 1L;

        ExportJob job = exportJobRepository.findByTenantIdAndJobCode(tenantId, jobCode)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Yêu cầu xuất báo cáo với mã: " + jobCode));

        if (job.getStatus() != ExportJobStatus.READY) {
            throw new IllegalStateException("File báo cáo chưa sẵn sàng hoặc đã thất bại. Trạng thái hiện tại: " + job.getStatus());
        }

        if (job.getExpiresAt() != null && job.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("File báo cáo đã hết hạn tải về. Vui lòng thực hiện tạo yêu cầu mới.");
        }

        try {
            return reportStorageService.loadFile(job.getFilePath());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đọc file báo cáo từ lưu trữ: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExportJobResponse> getExportHistory(Pageable pageable) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) tenantId = 1L;

        return exportJobRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
            .map(this::mapToResponse);
    }

    @Scheduled(cron = "0 0 * * * *") // Runs hourly
    @Transactional
    public void cleanupExpiredJobs() {
        LocalDateTime now = LocalDateTime.now();
        List<ExportJob> expiredJobs = exportJobRepository.findByStatusAndExpiresAtBefore(ExportJobStatus.READY, now);

        for (ExportJob job : expiredJobs) {
            log.info("Cleaning up expired ExportJob: {}", job.getJobCode());
            reportStorageService.deleteFile(job.getFilePath());
            job.setStatus(ExportJobStatus.EXPIRED);
            exportJobRepository.save(job);
        }
    }

    private ExportJobResponse mapToResponse(ExportJob job) {
        String downloadUrl = null;
        if (job.getStatus() == ExportJobStatus.READY) {
            downloadUrl = "/api/v1/reports/export-jobs/" + job.getJobCode() + "/download";
        }

        return ExportJobResponse.builder()
            .jobId(job.getJobCode())
            .reportCode(job.getReportCode())
            .format(job.getFormat())
            .status(job.getStatus())
            .fileName(job.getFileName())
            .fileSize(job.getFileSize())
            .recordCount(job.getRecordCount())
            .errorMessage(job.getErrorMessage())
            .requestedBy(job.getRequestedBy())
            .createdAt(job.getCreatedAt())
            .completedAt(job.getCompletedAt())
            .expiresAt(job.getExpiresAt())
            .downloadUrl(downloadUrl)
            .build();
    }
}
