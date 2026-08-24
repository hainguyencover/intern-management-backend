package com.holaho.intern.evaluation.controller;

import com.holaho.intern.evaluation.dto.FinalReportDetailResponse;
import com.holaho.intern.evaluation.dto.GenerateFinalReportRequest;
import com.holaho.intern.evaluation.enums.FinalReportStatus;
import com.holaho.intern.evaluation.export.ExcelReportExporter;
import com.holaho.intern.evaluation.export.PdfReportExporter;
import com.holaho.intern.evaluation.service.FinalReportModuleService;
import com.holaho.intern.shared.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for Final Evaluation Reports (US-020).
 *
 * Endpoints:
 *   POST   /api/v2/final-reports            — Generate report (HR/Admin)
 *   GET    /api/v2/final-reports/{id}        — Report detail (All)
 *   GET    /api/v2/final-reports/intern/{id} — Report by intern (All)
 *   GET    /api/v2/final-reports            — Paginated list (HR/Admin)
 *   POST   /api/v2/final-reports/{id}/approve — Approve report (HR/Admin)
 *   POST   /api/v2/final-reports/{id}/return  — Return report (HR/Admin)
 *   GET    /api/v2/final-reports/{id}/export/pdf — Export PDF
 *   GET    /api/v2/final-reports/{id}/export/excel — Export Excel/CSV
 */
@RestController
@RequestMapping("/api/v2/final-reports")
@RequiredArgsConstructor
public class FinalReportController {

    private final FinalReportModuleService reportService;
    private final PdfReportExporter pdfExporter;
    private final ExcelReportExporter excelExporter;

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<FinalReportDetailResponse>> generateReport(
            @Valid @RequestBody GenerateFinalReportRequest request) {
        FinalReportDetailResponse report = reportService.generateReport(request.getInternId());
        return ResponseEntity.ok(ApiResponse.success("Báo cáo tổng kết đã được tạo thành công", report));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<ApiResponse<FinalReportDetailResponse>> getById(@PathVariable Long id) {
        FinalReportDetailResponse report = reportService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/intern/{internId}")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<ApiResponse<FinalReportDetailResponse>> getByIntern(@PathVariable Long internId) {
        FinalReportDetailResponse report = reportService.getByIntern(internId);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<FinalReportDetailResponse>>> getAllReports(
            @RequestParam(required = false) FinalReportStatus status,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        Page<FinalReportDetailResponse> page = reportService.getAllReports(status, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.successPage(page));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<FinalReportDetailResponse>> approveReport(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String comment = body != null ? body.get("comment") : null;
        FinalReportDetailResponse report = reportService.approveReport(id, comment);
        return ResponseEntity.ok(ApiResponse.success("Báo cáo tổng kết đã được phê duyệt", report));
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<FinalReportDetailResponse>> returnReport(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "");
        FinalReportDetailResponse report = reportService.returnReport(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Báo cáo tổng kết đã được trả lại để chỉnh sửa", report));
    }

    @GetMapping("/{id}/export/pdf")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        FinalReportDetailResponse report = reportService.getById(id);
        byte[] bytes = pdfExporter.export(report);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Final_Report_" + report.getReportNumber() + pdfExporter.getFileExtension())
                .contentType(MediaType.parseMediaType(pdfExporter.getContentType()))
                .body(bytes);
    }

    @GetMapping("/{id}/export/excel")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<byte[]> exportExcel(@PathVariable Long id) {
        FinalReportDetailResponse report = reportService.getById(id);
        byte[] bytes = excelExporter.export(report);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Final_Report_" + report.getReportNumber() + excelExporter.getFileExtension())
                .contentType(MediaType.parseMediaType(excelExporter.getContentType()))
                .body(bytes);
    }
}
