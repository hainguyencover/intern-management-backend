package com.holaho.intern.controller;

import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.dto.request.AssignRolesRequest;
import com.holaho.intern.shared.dto.request.QrLogDto;


import com.holaho.intern.shared.dto.request.CreateUserRequest;
import com.holaho.intern.shared.dto.request.UpdateUserStatusRequest;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.AuditLogResponse;
import com.holaho.intern.shared.dto.response.UserResponse;
import com.holaho.intern.entity.BackupJob;
import com.holaho.intern.user.service.AdminUserService;
import com.holaho.intern.service.AuditLogService;
import com.holaho.intern.service.BackupService;
import com.holaho.intern.service.HrmService;
import com.holaho.intern.service.AttendanceService;
import com.holaho.intern.shared.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Admin Controller - User Management, RBAC, System Ops
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
public class AdminController {

    private final AdminUserService adminService;
    private final AuditLogService auditLogService;
    private final BackupService backupService;
    private final HrmService hrmService;
    private final AttendanceService attendanceService;

    // ========== USER MANAGEMENT ==========

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserResponse> users = adminService.getAllUsers(role, status, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse user = adminService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = adminService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success("User created successfully", user));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        UserResponse user = adminService.updateUserStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", user));
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(@PathVariable Long id) {
        String newPassword = adminService.resetUserPassword(id);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", Map.of("password", newPassword)));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    @PutMapping("/users/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateUserRoles(
            @PathVariable Long id,
            @RequestBody com.holaho.intern.shared.dto.request.AssignRolesRequest request) {
        adminService.assignRoles(id, request.getRoleCodes());
        return ResponseEntity.ok(ApiResponse.success("User roles updated successfully", null));
    }

    // ========== AUDIT LOGS ==========

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> searchAuditLogs(
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate toDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime to = (toDate != null) ? toDate.atTime(23, 59, 59) : null;

        Page<AuditLogResponse> logs = auditLogService.getAuditLogs(actorId, action, entityType, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    // ========== BACKUPS ==========

    @GetMapping("/backups")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<BackupJob>>> getBackups(
            @PageableDefault(size = 10, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(backupService.getBackupHistory(pageable)));
    }

    @PostMapping("/backups")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BackupJob>> triggerBackup(
            @AuthenticationPrincipal CustomUserDetails user) {
        BackupJob job = backupService.runManualBackup(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Backup triggered successfully", job));
    }

    @PostMapping("/hrm/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> syncHrm() {
        return ResponseEntity.ok(ApiResponse.success("HRM synchronization started", hrmService.syncData()));
    }

    @PostMapping("/attendance/sync/qr")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> syncQrLogs(
            @RequestBody java.util.List<com.holaho.intern.shared.dto.request.QrLogDto> logs) {
        return ResponseEntity
                .ok(ApiResponse.success("QR logs synchronization successful", attendanceService.syncQrData(logs)));
    }
}

