package com.holaho.intern.reporting;

import com.holaho.intern.reporting.dto.ReportData;
import com.holaho.intern.reporting.enums.ExportFormat;
import com.holaho.intern.reporting.service.exporter.PdfReportExporter;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PdfReportExporterTest {

    private final PdfReportExporter exporter = new PdfReportExporter();

    @Test
    void shouldExportPdfDocumentWithHeadersAndRows() {
        ReportData data = ReportData.builder()
            .reportCode("UNIVERSITY_STATISTICS")
            .reportName("Thống kê theo trường đại học")
            .category("Academic Reports")
            .columns(List.of("Tên trường", "Tổng số TTS", "Đã hoàn thành"))
            .rows(List.of(
                Map.of("Tên trường", "Đại học Bách Khoa", "Tổng số TTS", 50, "Đã hoàn thành", 35),
                Map.of("Tên trường", "Đại học Công Nghệ", "Tổng số TTS", 30, "Đã hoàn thành", 22)
            ))
            .summary(Map.of("Tổng số trường", 2))
            .generatedBy("hr.manager@company.com")
            .generatedAt(LocalDateTime.now())
            .build();

        byte[] result = exporter.export(data);

        assertNotNull(result);
        assertTrue(result.length > 0);
        assertEquals(ExportFormat.PDF, exporter.getFormat());
    }
}
