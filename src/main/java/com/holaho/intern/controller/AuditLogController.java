package com.holaho.intern.controller;

import com.holaho.intern.security.SecurityAuditContext;
import com.holaho.intern.service.AuditLogService;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.AuditLogResponse;
import com.holaho.intern.shared.annotation.Auditable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_READ') or hasAuthority('AUDIT_LOG_READ')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String actorUsername,
            @RequestParam(required = false) String actorRole,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Long tenantId = SecurityAuditContext.getTenantId();
        String effectiveResource = resourceType != null ? resourceType : entityType;

        Page<AuditLogResponse> logs = auditLogService.getAuditLogs(
                tenantId, actorId, actorUsername, actorRole, action, effectiveResource, result, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuditLogResponse>> getAuditLogById(@PathVariable Long id) {
        AuditLogResponse auditLog = auditLogService.getAuditLogById(id);
        return ResponseEntity.ok(ApiResponse.success(auditLog));
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_LOG_EXPORT')")
    @Auditable(action = "EXPORT_AUDIT_LOGS", resource = "AUDIT_LOG")
    public ResponseEntity<byte[]> exportAuditLogs(
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String actorUsername,
            @RequestParam(required = false) String actorRole,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        Long tenantId = SecurityAuditContext.getTenantId();
        Pageable exportLimit = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<AuditLogResponse> logs = auditLogService.getAuditLogs(
                tenantId, actorId, actorUsername, actorRole, action, resourceType, result, from, to, exportLimit);

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Timestamp,ActorUsername,ActorRole,Action,Resource,Result,IPAddress,RequestID\n");

        for (AuditLogResponse item : logs.getContent()) {
            csv.append(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    item.getId(),
                    item.getCreatedAt() != null ? item.getCreatedAt().toString() : "",
                    escapeCsv(item.getActorUsername()),
                    escapeCsv(item.getActorRole()),
                    escapeCsv(item.getAction()),
                    escapeCsv(item.getResourceType() != null ? item.getResourceType() : item.getEntityType()),
                    escapeCsv(item.getResult() != null ? item.getResult() : item.getStatus()),
                    escapeCsv(item.getIpAddress()),
                    escapeCsv(item.getRequestId())));
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "audit_logs_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    private String escapeCsv(String input) {
        if (input == null) return "";
        return input.replace("\"", "\"\"");
    }
}
