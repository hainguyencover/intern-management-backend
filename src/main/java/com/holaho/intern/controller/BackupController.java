package com.holaho.intern.controller;

import com.holaho.intern.security.SecurityAuditContext;
import com.holaho.intern.service.BackupService;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.BackupJobResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/admin/backups", "/api/v1/admin/system/backups"})
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasAuthority('BACKUP_READ') or hasAuthority('BACKUP:READ')")
public class BackupController {

    private final BackupService backupService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BackupJobResponse>>> getBackupHistory(
            @PageableDefault(size = 15, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<BackupJobResponse> history = backupService.getBackupHistory(pageable);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BackupJobResponse>> getBackupJobById(@PathVariable Long id) {
        BackupJobResponse job = backupService.getBackupJobById(id);
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('BACKUP_CREATE') or hasAuthority('BACKUP:CREATE')")
    public ResponseEntity<ApiResponse<BackupJobResponse>> triggerManualBackup(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody(required = false) Map<String, String> payload) {

        Long userId = user != null ? user.getId() : SecurityAuditContext.getActorId();
        String backupType = payload != null ? payload.getOrDefault("backupType", "FULL") : "FULL";

        log.info("Manual backup triggered by actor ID: {}", userId);
        BackupJobResponse job = backupService.runManualBackup(userId, backupType);
        return ResponseEntity.ok(ApiResponse.success("Backup triggered successfully", job));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('BACKUP_RESTORE') or hasAuthority('BACKUP:RESTORE')")
    public ResponseEntity<ApiResponse<Void>> restoreBackup(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {

        String confirmationCode = payload != null ? payload.get("confirmationCode") : null;
        log.warn("Restore operation initiated for backup ID: {}", id);

        backupService.restoreBackup(id, confirmationCode);
        return ResponseEntity.ok(ApiResponse.success("Database restore executed successfully", null));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('BACKUP_DOWNLOAD') or hasAuthority('BACKUP:DOWNLOAD')")
    public ResponseEntity<Resource> downloadBackupFile(@PathVariable Long id) {
        BackupJobResponse job = backupService.getBackupJobById(id);
        if (job.getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        File file = new File(job.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .body(resource);
    }
}
