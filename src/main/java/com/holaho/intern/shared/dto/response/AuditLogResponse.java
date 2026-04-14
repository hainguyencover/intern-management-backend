package com.holaho.intern.shared.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogResponse {
    private Long id;
    private Long actorId;
    private String actorEmail;
    private String action;
    private String entityType;
    private Long entityId;
    private String status;
    private String ipAddress;
    private String message;
    private String beforeJson;
    private String afterJson;
    private LocalDateTime createdAt;
}

