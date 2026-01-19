package com.example.backend.controller;

import com.example.backend.entity.AuditLog;
import com.example.backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Audit Log Controller - View System Activity Logs
 * Endpoints: /api/audit-logs
 * Required: ROLE_ADMIN or AUDIT_READ permission
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_READ')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * Get audit logs with filters
     * GET /api/audit-logs?actorId=&action=&entityType=&from=&to=&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("Get audit logs - actorId: {}, action: {}, entityType: {}", actorId, action, entityType);
        Page<AuditLog> logs = auditLogService.getAuditLogs(actorId, action, entityType, from, to, pageable);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get audit log by ID
     * GET /api/audit-logs/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuditLog> getAuditLogById(@PathVariable Long id) {
        log.info("Get audit log by ID: {}", id);
        AuditLog log = auditLogService.getAuditLogById(id);
        return ResponseEntity.ok(log);
    }
}
