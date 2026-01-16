package com.example.backend.service;

import com.example.backend.dto.response.AuditLogResponse;
import com.example.backend.entity.AuditLog;
import com.example.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public Page<AuditLogResponse> getAuditLogs(
            Long actorId,
            String action,
            String entityType,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {
        Page<AuditLog> logs = auditLogRepository.findByFilters(
                actorId, action, entityType, fromDate, toDate, pageable);
        return logs.map(this::mapToResponse);
    }

    public AuditLogResponse getAuditLogById(Long id) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Audit log không tồn tại"));
        return mapToResponse(log);
    }

    public void createAuditLog(
            Long actorId,
            String actorEmail,
            String action,
            String entityType,
            Long entityId,
            String message,
            String beforeJson,
            String afterJson) {
        AuditLog log = new AuditLog();
        log.setActorId(actorId);
        log.setActorEmail(actorEmail);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setMessage(message);
        log.setBeforeJson(beforeJson);
        log.setAfterJson(afterJson);
        log.setStatus("SUCCESS");
        log.setCreatedAt(LocalDateTime.now());

        // Note: IP address handling might need RequestContextHolder or passed argument
        // For now leaving null or "N/A"
        log.setIpAddress("N/A");

        auditLogRepository.save(log);
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(log.getId());
        response.setActorId(log.getActorId());
        response.setActorEmail(log.getActorEmail());
        response.setAction(log.getAction());
        response.setEntityType(log.getEntityType());
        response.setEntityId(log.getEntityId());
        response.setStatus(log.getStatus());
        response.setIpAddress(log.getIpAddress());
        response.setMessage(log.getMessage());
        response.setBeforeJson(log.getBeforeJson());
        response.setAfterJson(log.getAfterJson());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
}
