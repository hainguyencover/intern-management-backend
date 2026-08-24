package com.holaho.intern.attendance.controller;

import com.holaho.intern.attendance.dto.CreateLeaveRequestDto;
import com.holaho.intern.attendance.dto.LeaveDetailResponse;
import com.holaho.intern.attendance.dto.ReviewLeaveRequestDto;
import com.holaho.intern.attendance.entity.LeaveType;
import com.holaho.intern.attendance.service.LeaveRequestModuleService;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.enums.LeaveStatus;
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
@RequestMapping("/api/v2/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestV2Controller {

    private final LeaveRequestModuleService leaveService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<LeaveDetailResponse>> createLeaveRequest(
            @Valid @RequestBody CreateLeaveRequestDto req,
            @AuthenticationPrincipal CustomUserDetails user) {
        Long internId = userService.getInternProfileIdByUserId(user.getId());
        LeaveDetailResponse response = leaveService.createLeaveRequest(internId, req);
        return ResponseEntity.ok(ApiResponse.success("Đã gửi đơn xin nghỉ phép", response));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<List<LeaveDetailResponse>>> getMyLeaveRequests(
            @AuthenticationPrincipal CustomUserDetails user) {
        Long internId = userService.getInternProfileIdByUserId(user.getId());
        List<LeaveDetailResponse> list = leaveService.getMyLeaveRequests(internId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'MENTOR')")
    public ResponseEntity<ApiResponse<List<LeaveDetailResponse>>> getLeaveRequests(
            @RequestParam(required = false) LeaveStatus status,
            Pageable pageable) {
        Page<LeaveDetailResponse> page = leaveService.getLeaveRequests(status, pageable);
        return ResponseEntity.ok(ApiResponse.successPage(page));
    }

    @GetMapping("/types")
    @PreAuthorize("hasAnyRole('INTERN', 'HR', 'ADMIN', 'MENTOR')")
    public ResponseEntity<ApiResponse<List<LeaveType>>> getLeaveTypes() {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getLeaveTypes()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<LeaveDetailResponse>> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        LeaveDetailResponse response = leaveService.approveLeaveRequest(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Đơn nghỉ phép đã được phê duyệt", response));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<LeaveDetailResponse>> reject(
            @PathVariable Long id,
            @RequestBody(required = false) ReviewLeaveRequestDto req,
            @AuthenticationPrincipal CustomUserDetails user) {
        LeaveDetailResponse response = leaveService.rejectLeaveRequest(id, user.getId(), req);
        return ResponseEntity.ok(ApiResponse.success("Đơn nghỉ phép đã bị từ chối", response));
    }
}
