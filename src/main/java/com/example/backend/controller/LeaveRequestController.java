package com.example.backend.controller;

import com.example.backend.dto.request.LeaveRequestCreateRequest;
import com.example.backend.dto.request.LeaveRequestRejectRequest;
import com.example.backend.dto.response.LeaveRequestResponse;
import com.example.backend.entity.LeaveRequest;
import com.example.backend.security.CustomUserDetails;
import com.example.backend.service.LeaveRequestService;
import com.example.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<LeaveRequestResponse> createLeaveRequest(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody LeaveRequestCreateRequest request) {
        Long internId = userService.getInternProfileIdByUserId(user.getId());
        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(
                internId, request.getStartDate(), request.getEndDate(), request.getReason(), request.getLeaveType());
        return ResponseEntity.ok(LeaveRequestResponse.from(leaveRequest));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<Page<LeaveRequestResponse>> getAllRequests(
            @RequestParam(required = false) com.example.backend.enums.LeaveStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        // Use search to filter by status (internId null means all interns)
        Page<LeaveRequest> page = leaveRequestService.searchLeaveRequests(null, status, pageable);
        return ResponseEntity.ok(page.map(LeaveRequestResponse::from));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<Page<LeaveRequestResponse>> getMyLeaveRequests(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Long internId = userService.getInternProfileIdByUserId(user.getId());
        Page<LeaveRequest> page = leaveRequestService.getMyLeaveRequests(internId, pageable);
        return ResponseEntity.ok(page.map(LeaveRequestResponse::from));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<Page<LeaveRequestResponse>> getPendingLeaveRequests(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Page<LeaveRequest> page = leaveRequestService.getPendingLeaveRequests(pageable);
        return ResponseEntity.ok(page.map(LeaveRequestResponse::from));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<LeaveRequestResponse> approveLeaveRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        LeaveRequest leaveRequest = leaveRequestService.approveLeaveRequest(id, user.getId());
        return ResponseEntity.ok(LeaveRequestResponse.from(leaveRequest));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<LeaveRequestResponse> rejectLeaveRequest(
            @PathVariable Long id,
            @Valid @RequestBody LeaveRequestRejectRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        LeaveRequest leaveRequest = leaveRequestService.rejectLeaveRequest(
                id, user.getId(), request.getReason());
        return ResponseEntity.ok(LeaveRequestResponse.from(leaveRequest));
    }
}
