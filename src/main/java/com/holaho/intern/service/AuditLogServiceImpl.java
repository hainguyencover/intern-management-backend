package com.holaho.intern.service;

import com.holaho.intern.entity.AuditLog;
import com.holaho.intern.repository.AuditLogRepository;
import com.holaho.intern.shared.dto.response.AuditLogResponse;
import com.holaho.intern.shared.events.AuditEvent;
import com.holaho.intern.shared.mapper.AuditLogMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        createAuditLog(actorId, actorEmail, action, entityType, entityId, status, message, null, null, null, null);
    }

    @Override
    @Async
    @Transactional
    public void createAuditLog(Long actorId, String actorEmail, String action, String entityType,
                                Long entityId, String status, String message, String beforeJson, String afterJson,
                                String ipAddress, String userAgent) {
        AuditEvent event = AuditEvent.builder()
                .actorId(actorId)
                .actorEmail(actorEmail)
                .action(action)
                .entityType(entityType)
                .resourceType(entityType)
                .entityId(entityId)
                .resourceId(entityId != null ? String.valueOf(entityId) : null)
                .status(status != null ? status : "SUCCESS")
                .result(status != null ? status : "SUCCESS")
                .message(message)
                .beforeJson(beforeJson)
                .afterJson(afterJson)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        createAuditLog(event);
    }

    @Override
    @Transactional
    public void createAuditLog(AuditEvent event) {
        log.debug("Persisting audit log for action: {}", event.getAction());
        AuditLog auditLog = AuditLog.builder()
                .tenantId(event.getTenantId())
                .actorId(event.getActorId())
                .actorUsername(event.getActorUsername())
                .actorEmail(event.getActorEmail())
                .actorRole(event.getActorRole())
                .action(event.getAction())
                .resourceType(event.getResourceType() != null ? event.getResourceType() : event.getEntityType())
                .resourceId(event.getResourceId() != null ? event.getResourceId() : (event.getEntityId() != null ? String.valueOf(event.getEntityId()) : null))
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .result(event.getResult() != null ? event.getResult() : (event.getStatus() != null ? event.getStatus() : "SUCCESS"))
                .status(event.getStatus() != null ? event.getStatus() : "SUCCESS")
                .ipAddress(event.getIpAddress())
                .userAgent(event.getUserAgent())
                .requestId(event.getRequestId())
                .message(event.getMessage())
                .beforeJson(event.getBeforeJson())
                .afterJson(event.getAfterJson())
                .oldValue(event.getOldValue())
                .newValue(event.getNewValue())
                .metadata(event.getMetadata())
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(
            Long tenantId,
            Long actorId,
            String actorUsername,
            String actorRole,
            String action,
            String resourceType,
            String result,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {

        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (tenantId != null) {
                predicates.add(cb.equal(root.get("tenantId"), tenantId));
            }
            if (actorId != null) {
                predicates.add(cb.equal(root.get("actorId"), actorId));
            }
            if (actorUsername != null && !actorUsername.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("actorUsername")), "%" + actorUsername.trim().toLowerCase() + "%"));
            }
            if (actorRole != null && !actorRole.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("actorRole"), actorRole.trim()));
            }
            if (action != null && !action.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("action"), action.trim()));
            }
            if (resourceType != null && !resourceType.trim().isEmpty()) {
                predicates.add(cb.or(
                        cb.equal(root.get("resourceType"), resourceType.trim()),
                        cb.equal(root.get("entityType"), resourceType.trim())
                ));
            }
            if (result != null && !result.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("result"), result.trim()));
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
                .orElseThrow(() -> new RuntimeException("Audit log not found with ID: " + id));
        return auditLogMapper.toResponse(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getEntityHistory(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream().map(auditLogMapper::toResponse).toList();
    }
}
