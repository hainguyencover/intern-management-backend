package com.holaho.intern.attendance.controller;

import com.holaho.intern.attendance.dto.CorrectionResponse;
import com.holaho.intern.attendance.dto.CreateCorrectionRequestDto;
import com.holaho.intern.attendance.dto.ReviewCorrectionRequestDto;
import com.holaho.intern.attendance.enums.CorrectionStatus;
import com.holaho.intern.attendance.service.AttendanceCorrectionService;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/attendance/corrections")
@RequiredArgsConstructor
public class AttendanceCorrectionController {

    private final AttendanceCorrectionService correctionService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<CorrectionResponse>> requestCorrection(
            @Valid @RequestBody CreateCorrectionRequestDto req,
            @AuthenticationPrincipal CustomUserDetails user) {
        Long internId = userService.getInternProfileIdByUserId(user.getId());
        CorrectionResponse response = correctionService.createCorrectionRequest(internId, req);
        return ResponseEntity.ok(ApiResponse.success("Đã gửi yêu cầu sửa công", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CorrectionResponse>>> getCorrections(
            @RequestParam(required = false) CorrectionStatus status,
            Pageable pageable) {
        Page<CorrectionResponse> page = correctionService.getCorrections(status, pageable);
        return ResponseEntity.ok(ApiResponse.successPage(page));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<CorrectionResponse>> approve(
            @PathVariable Long id,
            @RequestBody(required = false) ReviewCorrectionRequestDto req,
            @AuthenticationPrincipal CustomUserDetails user) {
        CorrectionResponse response = correctionService.approveCorrection(id, user.getId(), req);
        return ResponseEntity.ok(ApiResponse.success("Yêu cầu sửa công đã được duyệt", response));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<CorrectionResponse>> reject(
            @PathVariable Long id,
            @RequestBody(required = false) ReviewCorrectionRequestDto req,
            @AuthenticationPrincipal CustomUserDetails user) {
        CorrectionResponse response = correctionService.rejectCorrection(id, user.getId(), req);
        return ResponseEntity.ok(ApiResponse.success("Yêu cầu sửa công đã bị từ chối", response));
    }
}
