package com.holaho.intern.reporting;

import com.holaho.intern.reporting.dto.ReportData;
import com.holaho.intern.reporting.enums.ExportFormat;
import com.holaho.intern.reporting.service.exporter.ExcelReportExporter;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExcelReportExporterTest {

    private final ExcelReportExporter exporter = new ExcelReportExporter();

    @Test
    void shouldExportExcelFileWithHeadersAndRows() {
        ReportData data = ReportData.builder()
            .reportCode("INTERN_LIST")
            .reportName("Danh sách thực tập sinh")
            .category("HR Reports")
            .columns(List.of("Họ và tên", "Mã sinh viên", "Trạng thái"))
            .rows(List.of(
                Map.of("Họ và tên", "Nguyễn Văn A", "Mã sinh viên", "SV001", "Trạng thái", "ACTIVE"),
                Map.of("Họ và tên", "Trần Thị B", "Mã sinh viên", "SV002", "Trạng thái", "COMPLETED")
            ))
            .summary(Map.of("Tổng số thực tập sinh", 2))
            .generatedBy("hr.admin@company.com")
            .generatedAt(LocalDateTime.now())
            .build();

        byte[] result = exporter.export(data);

        assertNotNull(result);
        assertTrue(result.length > 0);
        assertEquals(ExportFormat.XLSX, exporter.getFormat());
    }
}
