package com.holaho.intern.reporting.service;

import com.holaho.intern.reporting.dto.ReportCatalogDto;
import com.holaho.intern.reporting.dto.ReportData;
import com.holaho.intern.reporting.dto.ReportFilterRequest;
import com.holaho.intern.reporting.dto.ReportPreviewResponse;
import com.holaho.intern.reporting.enums.ExportFormat;
import com.holaho.intern.reporting.service.generator.ReportGenerator;
import com.holaho.intern.reporting.service.generator.ReportGeneratorRegistry;
import com.holaho.intern.shared.config.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ReportGeneratorRegistry generatorRegistry;

    @Override
    public List<ReportCatalogDto> getReportCatalog() {
        return generatorRegistry.getAllGenerators().stream()
            .map(gen -> ReportCatalogDto.builder()
                .code(gen.getReportCode())
                .name(gen.getReportName())
                .description(gen.getDescription())
                .category(gen.getCategory())
                .supportedFormats(List.of(ExportFormat.XLSX, ExportFormat.PDF))
                .availableFilters(gen.getAvailableFilters())
                .build())
            .toList();
    }

    @Override
    public ReportPreviewResponse previewReport(String reportCode, ReportFilterRequest filter) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) tenantId = 1L;

        ReportGenerator generator = generatorRegistry.get(reportCode);
        ReportData reportData = generator.generate(filter != null ? filter : new ReportFilterRequest(), tenantId);

        return ReportPreviewResponse.builder()
            .reportCode(reportData.getReportCode())
            .reportName(reportData.getReportName())
            .totalRecords(reportData.getRecordCount())
            .columns(reportData.getColumns())
            .rows(reportData.getRows())
            .summary(reportData.getSummary())
            .generatedAt(LocalDateTime.now())
            .build();
    }
}
