package com.holaho.intern.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupJobResponse {
    private Long id;
    private Long tenantId;
    private String backupType;
    private String type;
    private String status;
    private String filePath;
    private String storagePath;
    private Long fileSize;
    private String checksum;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime retentionUntil;
    private String errorCode;
    private String errorMessage;
    private String message;
    private String createdByUsername;
    private LocalDateTime createdAt;
}
