package com.holaho.intern.service;

import com.holaho.intern.entity.BackupJob;
import com.holaho.intern.repository.BackupJobRepository;
import com.holaho.intern.security.SecurityAuditContext;
import com.holaho.intern.shared.annotation.Auditable;
import com.holaho.intern.shared.dto.response.BackupJobResponse;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    private final BackupJobRepository backupJobRepository;
    private final UserRepository userRepository;
    private final BackupExecutionService backupExecutionService;
    private final RestoreService restoreService;

    @Transactional
    @Auditable(action = "TRIGGER_MANUAL_BACKUP", resource = "BACKUP_JOB")
    public BackupJobResponse runManualBackup(Long userId, String backupType) {
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }
        Long tenantId = SecurityAuditContext.getTenantId();

        BackupJob job = backupExecutionService.executeBackupJob("MANUAL", backupType != null ? backupType : "FULL", tenantId, user);
        return mapToResponse(job);
    }

    @Transactional(readOnly = true)
    public Page<BackupJobResponse> getBackupHistory(Pageable pageable) {
        return backupJobRepository.findAllOrderByStartedAtDesc(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public BackupJobResponse getBackupJobById(Long id) {
        BackupJob job = backupJobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Backup job not found with ID: " + id));
        return mapToResponse(job);
    }

    public void restoreBackup(Long id, String confirmationCode) {
        restoreService.restoreFromBackup(id, confirmationCode);
    }

    private BackupJobResponse mapToResponse(BackupJob job) {
        return BackupJobResponse.builder()
                .id(job.getId())
                .tenantId(job.getTenantId())
                .backupType(job.getBackupType())
                .type(job.getType())
                .status(job.getStatus())
                .filePath(job.getFilePath())
                .storagePath(job.getStoragePath())
                .fileSize(job.getFileSize())
                .checksum(job.getChecksum())
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .retentionUntil(job.getRetentionUntil())
                .errorCode(job.getErrorCode())
                .errorMessage(job.getErrorMessage())
                .message(job.getMessage())
                .createdByUsername(job.getCreatedBy() != null ? job.getCreatedBy().getUsername() : "SYSTEM")
                .createdAt(job.getCreatedAt())
                .build();
    }
}
