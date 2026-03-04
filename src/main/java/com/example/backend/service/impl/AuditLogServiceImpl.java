package com.example.backend.service.impl;

import com.example.backend.dto.response.AuditLogResponse;
import com.example.backend.entity.AuditLog;
import com.example.backend.mapper.AuditLogMapper;
import com.example.backend.repository.AuditLogRepository;
import com.example.backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Override
    @Transactional
    public void log(Long actorId, String actorEmail, String action, String entityType,
            Long entityId, String status, String message) {
        // The original createAuditLog set status to "SUCCESS".
        // The new createAuditLog does not set status.
        // For consistency with the original behavior of `log` method,
        // we pass the status parameter to the new createAuditLog.
        createAuditLog(actorId, actorEmail, action, entityType, entityId, status, message, null, null, null, null);
    }

    @Override
    @Async
    @Transactional
    public void createAuditLog(Long actorId, String actorEmail, String action, String entityType,
            Long entityId, String status, String message, String beforeJson, String afterJson, String ipAddress,
            String userAgent) {
        log.info("Saving async audit log for action: {}", action);
        AuditLog auditLog = new AuditLog();
        auditLog.setActorId(actorId);
        auditLog.setActorEmail(actorEmail);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setStatus(status); // Set status from parameter
        auditLog.setMessage(message);
        auditLog.setBeforeJson(beforeJson);
        auditLog.setAfterJson(afterJson);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        auditLog.setCreatedAt(LocalDateTime.now());

        auditLogRepository.save(auditLog);
        log.debug("Audit log created: {} on {} (ID: {})", action, entityType, entityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(
            Long actorId,
            String action,
            String entityType,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (actorId != null) {
                predicates.add(cb.equal(root.get("actorId"), actorId));
            }

            if (action != null && !action.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("action"), action));
            }

            if (entityType != null && !entityType.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("entityType"), entityType));
            }

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }

            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }

            query.orderBy(cb.desc(root.get("createdAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return auditLogRepository.findAll(spec, pageable).map(auditLogMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLogById(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audit log not found: " + id));
        return auditLogMapper.toResponse(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getEntityHistory(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream().map(auditLogMapper::toResponse).toList();
    }
}
