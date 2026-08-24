package com.holaho.intern.controller;

import com.holaho.intern.service.ContractService;
import com.holaho.intern.shared.dto.request.ContractRevisionRequest;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.ContractResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    // ==========================================
    // HR ENDPOINTS (US-009 / US-048 / US-051)
    // ==========================================

    @PostMapping("/hr/applications/{applicationId}/contract")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<ContractResponse>> uploadContractByHr(
            @PathVariable Long applicationId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ContractResponse response = contractService.uploadContract(applicationId, file, userDetails != null ? userDetails.getId() : null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tải lên hoặc thay thế hợp đồng thực tập thành công", response));
    }

    @PostMapping("/hr/contracts/{id}/confirm")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<ContractResponse>> confirmContractByHr(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ContractResponse response = contractService.confirmByHr(id, userDetails != null ? userDetails.getId() : null);
        return ResponseEntity.ok(ApiResponse.success("HR xác nhận hợp đồng thành công", response));
    }

    // ==========================================
    // INTERN ENDPOINTS (US-010 / US-060 / US-061)
    // ==========================================

    @GetMapping("/intern/contracts/current")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<ContractResponse>> getCurrentInternContract(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ContractResponse> myContracts = contractService.getMyContracts(userDetails.getId());
        if (myContracts.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }
        return ResponseEntity.ok(ApiResponse.success(myContracts.get(0)));
    }

    @PostMapping("/intern/contracts/{id}/confirm")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<ContractResponse>> confirmContractByIntern(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        ContractResponse response = contractService.confirmByIntern(id, userDetails.getId(), clientIp, userAgent);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận ký hợp đồng thành công", response));
    }

    @PostMapping("/intern/contracts/{id}/request-revision")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<ContractResponse>> requestRevisionByIntern(
            @PathVariable Long id,
            @Valid @RequestBody ContractRevisionRequest revisionRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ContractResponse response = contractService.requestRevision(id, userDetails.getId(), revisionRequest);
        return ResponseEntity.ok(ApiResponse.success("Đã gửi yêu cầu chỉnh sửa hợp đồng tới HR", response));
    }

    // ==========================================
    // SHARED PREVIEW & DOWNLOAD ENDPOINTS (US-047 / US-056)
    // ==========================================

    @GetMapping("/contracts/{id}/preview")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<Resource> previewContract(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean isHrOrAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HR") || a.getAuthority().equals("ROLE_ADMIN"));

        Resource resource = contractService.getContractFileResource(id, userDetails.getId(), isHrOrAdmin);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"contract-preview-" + id + ".pdf\"")
                .body(resource);
    }

    @GetMapping("/contracts/{id}/download")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<Resource> downloadContract(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean isHrOrAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HR") || a.getAuthority().equals("ROLE_ADMIN"));

        Resource resource = contractService.getContractFileResource(id, userDetails.getId(), isHrOrAdmin);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contract-" + id + ".pdf\"")
                .body(resource);
    }

    // ==========================================
    // LEGACY & COMPATIBILITY ENDPOINTS
    // ==========================================

    @GetMapping("/contracts")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ContractResponse>>> getAll() {
        List<ContractResponse> response = contractService.getAllContracts();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/contracts")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<ContractResponse>> uploadLegacy(
            @RequestParam Long applicationId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ContractResponse response = contractService.uploadContract(applicationId, file, userDetails != null ? userDetails.getId() : null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Hợp đồng được tải lên thành công", response));
    }

    @PutMapping("/contracts/{id}/sign")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<ContractResponse>> signLegacy(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        ContractResponse response = contractService.confirmByIntern(id, userDetails.getId(), clientIp, userAgent);
        return ResponseEntity.ok(ApiResponse.success("Ký hợp đồng thành công", response));
    }

    @GetMapping("/contracts/me")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<List<ContractResponse>>> getMyContractsLegacy(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ContractResponse> response = contractService.getMyContracts(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/contracts/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<ApiResponse<ContractResponse>> getLegacy(@PathVariable Long id) {
        ContractResponse response = contractService.getContract(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
