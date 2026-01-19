package com.example.backend.controller;

import com.example.backend.entity.BackupJob;
import com.example.backend.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
@Slf4j
public class BackupController {

    private final BackupService backupService;

    /**
     * Run manual backup
     * POST /api/admin/system/backup
     */
    @PostMapping("/backup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BackupJob> runManualBackup(
            @AuthenticationPrincipal com.example.backend.security.CustomUserDetails user) {
        log.info("Manual backup triggered by: {}", user.getUsername());

        BackupJob job = backupService.runManualBackup(user.getId());
        return ResponseEntity.ok(job);
    }

    /**
     * Get backup history
     * GET /api/admin/system/backups?page=0&size=10
     */
    @GetMapping("/backups")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('BACKUP_READ')")
    public ResponseEntity<Page<BackupJob>> getBackupHistory(
            @PageableDefault(size = 10, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Get backup history");
        Page<BackupJob> history = backupService.getBackupHistory(pageable);
        return ResponseEntity.ok(history);
    }

    /**
     * Cleanup old backups
     * DELETE /api/admin/system/backups/cleanup?days=30
     */
    @DeleteMapping("/backups/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cleanupOldBackups(
            @RequestParam(defaultValue = "30") int days) {
        log.info("Cleanup backups older than {} days", days);
        backupService.cleanupOldBackups(days);
        return ResponseEntity.ok().build();
    }
}
