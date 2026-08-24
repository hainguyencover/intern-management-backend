package com.holaho.intern.service;

import com.holaho.intern.shared.dto.response.AuditLogResponse;
import com.holaho.intern.shared.events.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogService {
    void log(Long actorId, String actorEmail, String action, String entityType, Long entityId, String status, String message);

    void createAuditLog(Long actorId, String actorEmail, String action, String entityType, Long entityId,
                        String status, String message, String beforeJson, String afterJson, String ipAddress,
                        String userAgent);

    void createAuditLog(AuditEvent event);

    Page<AuditLogResponse> getAuditLogs(Long tenantId, Long actorId, String actorUsername, String actorRole,
                                        String action, String resourceType, String result,
                                        LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);

    AuditLogResponse getAuditLogById(Long id);

    List<AuditLogResponse> getEntityHistory(String entityType, Long entityId);
}
