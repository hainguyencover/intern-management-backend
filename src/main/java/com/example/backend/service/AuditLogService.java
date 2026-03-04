package com.example.backend.service;

import com.example.backend.dto.response.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogService {
        void log(Long actorId, String actorEmail, String action, String entityType, Long entityId, String status,
                        String message);

        void createAuditLog(Long actorId, String actorEmail, String action, String entityType, Long entityId,
                        String status, String message, String beforeJson, String afterJson, String ipAddress,
                        String userAgent);

        Page<AuditLogResponse> getAuditLogs(Long actorId, String action, String entityType, LocalDateTime fromDate,
                        LocalDateTime toDate, Pageable pageable);

        AuditLogResponse getAuditLogById(Long id);

        List<AuditLogResponse> getEntityHistory(String entityType, Long entityId);
}
