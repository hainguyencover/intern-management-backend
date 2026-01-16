package com.example.backend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BackupJobResponse {
    private Long id;
    private String type;
    private String status;
    private String filePath;
    private Long fileSize;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String message;
}
