package com.example.backend.controller;

import com.example.backend.dto.request.CreateUserRequest;
import com.example.backend.dto.request.UpdateUserStatusRequest;
import com.example.backend.dto.response.UserResponse;
import com.example.backend.entity.AuditLog;
import com.example.backend.entity.BackupJob;
import com.example.backend.service.AdminUserService;
import com.example.backend.service.AuditLogService;
import com.example.backend.service.BackupService;
import com.example.backend.service.HrmService;
import com.example.backend.service.AttendanceService;
import com.example.backend.security.CustomUserDetails;
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

/**
 * Admin Controller - User Management, RBAC, System Ops
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
public class AdminController {

    private final AdminUserService adminService;
    private final AuditLogService auditLogService;
    private final BackupService backupService;
    private final HrmService hrmService;
    private final AttendanceService attendanceService;

    // ... (keep headers) ...

    // ========== USER MANAGEMENT ==========

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserResponse> users = adminService.getAllUsers(role, status, keyword, pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse user = adminService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = adminService.createUser(request);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        UserResponse user = adminService.updateUserStatus(id, request);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id) {
        adminService.resetUserPassword(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ========== AUDIT LOGS ==========

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLog>> searchAuditLogs(
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AuditLog> logs = auditLogService.getAuditLogs(actorId, action, entityType, fromDate, toDate, pageable);
        return ResponseEntity.ok(logs);
    }

    // ========== BACKUPS ==========

    @GetMapping("/backups")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<BackupJob>> getBackups(
            @PageableDefault(size = 10, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(backupService.getBackupHistory(pageable));
    }

    @PostMapping("/backups")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BackupJob> triggerBackup(
            @AuthenticationPrincipal CustomUserDetails user) {
        BackupJob job = backupService.runManualBackup(user.getId());
        return ResponseEntity.ok(job);
    }

    @PostMapping("/hrm/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> syncHrm() {
        return ResponseEntity.ok(hrmService.syncData());
    }

    @PostMapping("/attendance/sync/qr")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> syncQrLogs(
            @RequestBody java.util.List<com.example.backend.dto.request.QrLogDto> logs) {
        return ResponseEntity.ok(attendanceService.syncQrData(logs));
    }
}
