package com.example.backend.controller;

import com.example.backend.dto.response.BackupJobResponse;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.AuditLogService;
import com.example.backend.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @PostMapping("/backup")
    @PreAuthorize("hasAuthority('BACKUP_RUN')")
    public ResponseEntity<BackupJobResponse> runBackup(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        BackupJobResponse response = backupService.runBackupManually(user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/backup/history")
    @PreAuthorize("hasAuthority('BACKUP_READ')")
    public ResponseEntity<List<BackupJobResponse>> getBackupHistory() {
        return ResponseEntity.ok(backupService.getBackupHistory());
    }
}
