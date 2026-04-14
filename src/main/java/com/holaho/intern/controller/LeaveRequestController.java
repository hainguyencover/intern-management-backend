package com.holaho.intern.controller;

import com.holaho.intern.shared.enums.LeaveStatus;


import com.holaho.intern.shared.dto.request.LeaveRequestCreateRequest;
import com.holaho.intern.shared.dto.request.LeaveRequestRejectRequest;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.LeaveRequestResponse;
import com.holaho.intern.entity.LeaveRequest;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.service.LeaveRequestService;
import com.holaho.intern.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> createLeaveRequest(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody LeaveRequestCreateRequest request) {
        Long internId = userService.getInternProfileIdByUserId(user.getId());
        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(
                internId, request.getStartDate(), request.getEndDate(), request.getReason(), request.getLeaveType());
        return ResponseEntity.ok(
                ApiResponse.success("Leave request submitted successfully", LeaveRequestResponse.from(leaveRequest)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<LeaveRequestResponse>>> getAllRequests(
            @RequestParam(required = false) com.holaho.intern.shared.enums.LeaveStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<LeaveRequest> page = leaveRequestService.searchLeaveRequests(null, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(page.map(LeaveRequestResponse::from)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<Page<LeaveRequestResponse>>> getMyLeaveRequests(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Long internId = userService.getInternProfileIdByUserId(user.getId());
        Page<LeaveRequest> page = leaveRequestService.getMyLeaveRequests(internId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page.map(LeaveRequestResponse::from)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<LeaveRequestResponse>>> getPendingLeaveRequests(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Page<LeaveRequest> page = leaveRequestService.getPendingLeaveRequests(pageable);
        return ResponseEntity.ok(ApiResponse.success(page.map(LeaveRequestResponse::from)));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> approveLeaveRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        LeaveRequest leaveRequest = leaveRequestService.approveLeaveRequest(id, user.getId());
        return ResponseEntity.ok(
                ApiResponse.success("Leave request approved successfully", LeaveRequestResponse.from(leaveRequest)));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> rejectLeaveRequest(
            @PathVariable Long id,
            @Valid @RequestBody LeaveRequestRejectRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        LeaveRequest leaveRequest = leaveRequestService.rejectLeaveRequest(
                id, user.getId(), request.getReason());
        return ResponseEntity.ok(
                ApiResponse.success("Leave request rejected successfully", LeaveRequestResponse.from(leaveRequest)));
    }
}

