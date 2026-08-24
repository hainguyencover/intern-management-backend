package com.holaho.intern.integration.entity;

import com.holaho.intern.integration.enums.SyncStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "integration_sync_items",
    indexes = @Index(name = "idx_sync_items_job_status", columnList = "sync_job_id, status")
)
public class IntegrationSyncItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sync_job_id", nullable = false)
    private IntegrationSyncJob syncJob;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Column(name = "internal_id")
    private Long internalId;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(nullable = false, length = 30)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SyncStatus status;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
