package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_logs", indexes = {
                @Index(name = "idx_audit_time", columnList = "created_at"),
                @Index(name = "idx_audit_actor", columnList = "actor_id,created_at"),
                @Index(name = "idx_audit_action", columnList = "action,created_at"),
                @Index(name = "idx_audit_entity", columnList = "entity_type,entity_id")
})
public class AuditLog {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "actor_id")
        private Long actorId;

        @Column(name = "actor_email")
        private String actorEmail;

        @Column(nullable = false, length = 64)
        private String action;

        @Column(name = "entity_type", length = 64)
        private String entityType;

        @Column(name = "entity_id")
        private Long entityId;

        @Column(length = 16)
        private String status = "SUCCESS";

        @Column(name = "ip_address", length = 64)
        private String ipAddress;

        @Column(name = "user_agent")
        private String userAgent;

        @Column(name = "request_id", length = 64)
        private String requestId;

        @Column(columnDefinition = "TEXT")
        private String message;

        @Column(name = "before_json", columnDefinition = "TEXT")
        private String beforeJson;

        @Column(name = "after_json", columnDefinition = "TEXT")
        private String afterJson;

        @Column(name = "created_at", nullable = false)
        private LocalDateTime createdAt = LocalDateTime.now();
}
