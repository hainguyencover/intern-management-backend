package com.holaho.intern.reporting.controller;

import com.holaho.intern.reporting.dto.*;
import com.holaho.intern.reporting.enums.ExportFormat;
import com.holaho.intern.reporting.service.ExportJobService;
import com.holaho.intern.reporting.service.ReportExportService;
import com.holaho.intern.reporting.service.ReportService;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reporting Engine", description = "APIs for Report Catalog, Preview, Export Excel/PDF, Export Jobs, and History")
public class ReportController {

    private final ReportService reportService;
    private final ReportExportService reportExportService;
    private final ExportJobService exportJobService;

    @GetMapping
    @Operation(summary = "Lấy danh mục báo cáo", description = "Trả về danh sách tất cả các loại báo cáo được hỗ trợ trong hệ thống")
    @PreAuthorize("hasAnyAuthority('REPORT_VIEW', 'ROLE_ADMIN', 'ROLE_HR')")
    public ResponseEntity<ApiResponse<List<ReportCatalogDto>>> getReportCatalog() {
        List<ReportCatalogDto> catalog = reportService.getReportCatalog();
        return ResponseEntity.ok(ApiResponse.success(catalog, "Lấy danh mục báo cáo thành công"));
    }

    @PostMapping("/preview")
    @Operation(summary = "Xem trước dữ liệu báo cáo", description = "Trả về bảng dữ liệu và thông số tổng quan trước khi tải file báo cáo")
    @PreAuthorize("hasAnyAuthority('REPORT_VIEW', 'ROLE_ADMIN', 'ROLE_HR')")
    public ResponseEntity<ApiResponse<ReportPreviewResponse>> previewReport(
            @RequestParam String reportCode,
            @RequestBody(required = false) ReportFilterRequest filter) {
        ReportPreviewResponse preview = reportService.previewReport(reportCode, filter);
        return ResponseEntity.ok(ApiResponse.success(preview, "Xem trước dữ liệu báo cáo thành công"));
    }

    @PostMapping("/export")
    @Operation(summary = "Xuất báo cáo trực tiếp", description = "Xuất file Excel hoặc PDF trực tiếp cho các báo cáo dung lượng vừa/nhỏ")
    @PreAuthorize("hasAnyAuthority('REPORT_EXPORT', 'ROLE_ADMIN', 'ROLE_HR')")
    public ResponseEntity<byte[]> exportReportDirect(
            @Valid @RequestBody ReportExportRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getId() : 1L;
        String email = userDetails != null ? userDetails.getEmail() : "system";

        byte[] fileBytes = reportExportService.exportReportDirect(request, userId, email);

        ExportFormat format = request.getFormat();
        String extension = format.getFileExtension();
        String fileName = request.getReportCode().toLowerCase() + "_export" + extension;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(format.getContentType()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());
        headers.setContentLength(fileBytes.length);

        return ResponseEntity.ok().headers(headers).body(fileBytes);
    }

    @PostMapping("/export-jobs")
    @Operation(summary = "Tạo Yêu cầu Xuất báo cáo nền (Async Export Job)", description = "Tạo job xử lý nền cho các báo cáo dữ liệu lớn")
    @PreAuthorize("hasAnyAuthority('REPORT_EXPORT', 'ROLE_ADMIN', 'ROLE_HR')")
    public ResponseEntity<ApiResponse<ExportJobResponse>> createExportJob(
            @Valid @RequestBody ReportExportRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getId() : 1L;
        String email = userDetails != null ? userDetails.getEmail() : "system";

        ExportJobResponse jobResponse = exportJobService.createExportJob(request, userId, email);
        return ResponseEntity.ok(ApiResponse.success(jobResponse, "Khởi tạo yêu cầu xuất báo cáo thành công"));
    }

    @GetMapping("/export-jobs/{jobId}")
    @Operation(summary = "Kiểm tra tiến độ Export Job", description = "Lấy trạng thái xử lý của Job xuất báo cáo theo jobId")
    @PreAuthorize("hasAnyAuthority('REPORT_EXPORT', 'ROLE_ADMIN', 'ROLE_HR')")
    public ResponseEntity<ApiResponse<ExportJobResponse>> getExportJobStatus(@PathVariable String jobId) {
        ExportJobResponse jobResponse = exportJobService.getJobStatus(jobId);
        return ResponseEntity.ok(ApiResponse.success(jobResponse, "Lấy trạng thái yêu cầu thành công"));
    }

    @GetMapping("/export-jobs/{jobId}/download")
    @Operation(summary = "Tải file báo cáo đã hoàn thành", description = "Tải file báo cáo sau khi Export Job hoàn thành trạng thái READY")
    @PreAuthorize("hasAnyAuthority('REPORT_EXPORT', 'ROLE_ADMIN', 'ROLE_HR')")
    public ResponseEntity<byte[]> downloadJobFile(
            @PathVariable String jobId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getId() : 1L;

        ExportJobResponse status = exportJobService.getJobStatus(jobId);
        byte[] fileBytes = exportJobService.downloadJobFile(jobId, userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(status.getFormat().getContentType()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(status.getFileName()).build());
        headers.setContentLength(fileBytes.length);

        return ResponseEntity.ok().headers(headers).body(fileBytes);
    }

    @GetMapping("/export-history")
    @Operation(summary = "Xem lịch sử xuất báo cáo", description = "Trả về danh sách các lần xuất báo cáo và job trước đây của tenant")
    @PreAuthorize("hasAnyAuthority('REPORT_EXPORT_HISTORY', 'ROLE_ADMIN', 'ROLE_HR')")
    public ResponseEntity<ApiResponse<Page<ExportJobResponse>>> getExportHistory(Pageable pageable) {
        Page<ExportJobResponse> history = exportJobService.getExportHistory(pageable);
        return ResponseEntity.ok(ApiResponse.success(history, "Lấy lịch sử xuất báo cáo thành công"));
    }
}
