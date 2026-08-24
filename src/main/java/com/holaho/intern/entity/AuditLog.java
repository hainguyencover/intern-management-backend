package com.holaho.intern.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_time", columnList = "created_at"),
        @Index(name = "idx_audit_tenant_created", columnList = "tenant_id,created_at"),
        @Index(name = "idx_audit_actor_created", columnList = "actor_id,created_at"),
        @Index(name = "idx_audit_action_created", columnList = "action,created_at"),
        @Index(name = "idx_audit_resource", columnList = "resource_type,resource_id"),
        @Index(name = "idx_audit_request", columnList = "request_id")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_username", length = 255)
    private String actorUsername;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "actor_role", length = 100)
    private String actorRole;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "entity_type", length = 64)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(length = 30, nullable = false)
    @Builder.Default
    private String result = "SUCCESS"; // SUCCESS, FAILED, DENIED

    @Column(length = 16)
    @Builder.Default
    private String status = "SUCCESS";

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "before_json", columnDefinition = "TEXT")
    private String beforeJson;

    @Column(name = "after_json", columnDefinition = "TEXT")
    private String afterJson;

    @Column(name = "old_value", columnDefinition = "LONGTEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "LONGTEXT")
    private String newValue;

    @Column(name = "metadata", columnDefinition = "LONGTEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
