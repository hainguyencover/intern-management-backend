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
public class AuditLogResponse {
    private Long id;
    private Long tenantId;
    private Long actorId;
    private String actorUsername;
    private String actorEmail;
    private String actorRole;
    private String action;
    private String resourceType;
    private String resourceId;
    private String entityType;
    private Long entityId;
    private String result;
    private String status;
    private String ipAddress;
    private String userAgent;
    private String requestId;
    private String message;
    private String beforeJson;
    private String afterJson;
    private String oldValue;
    private String newValue;
    private String metadata;
    private LocalDateTime createdAt;
}
