package com.holaho.intern.reporting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.holaho.intern.reporting.dto.ReportData;
import com.holaho.intern.reporting.dto.ReportExportRequest;
import com.holaho.intern.reporting.dto.ReportFilterRequest;
import com.holaho.intern.reporting.enums.ExportFormat;
import com.holaho.intern.reporting.service.exporter.ExcelReportExporter;
import com.holaho.intern.reporting.service.exporter.PdfReportExporter;
import com.holaho.intern.reporting.service.generator.ReportGenerator;
import com.holaho.intern.reporting.service.generator.ReportGeneratorRegistry;
import com.holaho.intern.service.AuditLogService;
import com.holaho.intern.shared.config.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportExportServiceImpl implements ReportExportService {

    private final ReportGeneratorRegistry generatorRegistry;
    private final ExcelReportExporter excelReportExporter;
    private final PdfReportExporter pdfReportExporter;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Override
    public byte[] exportReportDirect(ReportExportRequest request, Long currentUserId, String userEmail) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) tenantId = 1L;

        ReportGenerator generator = generatorRegistry.get(request.getReportCode());
        ReportFilterRequest filter = request.getFilters() != null ? request.getFilters() : new ReportFilterRequest();
        ReportData reportData = generator.generate(filter, tenantId);

        if (userEmail != null) {
            reportData.setGeneratedBy(userEmail);
        }

        byte[] content;
        if (request.getFormat() == ExportFormat.XLSX) {
            content = excelReportExporter.export(reportData);
        } else {
            content = pdfReportExporter.export(reportData);
        }

        // Audit Logging
        try {
            String filterJson = objectMapper.writeValueAsString(filter);
            String payloadJson = objectMapper.writeValueAsString(Map.of(
                "reportCode", request.getReportCode(),
                "format", request.getFormat().name(),
                "recordCount", reportData.getRecordCount(),
                "filters", filterJson
            ));

            auditLogService.createAuditLog(
                currentUserId != null ? currentUserId : 1L,
                userEmail != null ? userEmail : "system",
                "REPORT_EXPORTED",
                "REPORT",
                null,
                "SUCCESS",
                "Xuất báo cáo " + generator.getReportName() + " (" + request.getFormat() + ") thành công với " + reportData.getRecordCount() + " bản ghi",
                null,
                payloadJson,
                null,
                null
            );
        } catch (Exception e) {
            log.warn("Lỗi khi ghi Audit Log xuất báo cáo: {}", e.getMessage());
        }

        return content;
    }
}
