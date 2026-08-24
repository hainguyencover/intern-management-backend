package com.holaho.intern.shared.events;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditEvent {
    private final Long tenantId;
    private final Long actorId;
    private final String actorUsername;
    private final String actorEmail;
    private final String actorRole;
    private final String action;
    private final String resourceType;
    private final String resourceId;
    private final String entityType;
    private final Long entityId;
    private final String result;
    private final String status;
    private final String message;
    private final String beforeJson;
    private final String afterJson;
    private final String oldValue;
    private final String newValue;
    private final String metadata;
    private final String ipAddress;
    private final String userAgent;
    private final String requestId;
}
