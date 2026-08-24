package com.holaho.intern.entity;

import com.holaho.intern.user.entity.User;
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
        name = "backup_jobs",
        indexes = {
                @Index(name = "idx_backup_status", columnList = "status"),
                @Index(name = "idx_backup_started_at", columnList = "started_at"),
                @Index(name = "idx_backup_retention", columnList = "retention_until")
        }
)
public class BackupJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "backup_type", length = 30, nullable = false)
    @Builder.Default
    private String backupType = "FULL"; // FULL, INCREMENTAL

    @Column(length = 32)
    @Builder.Default
    private String type = "MANUAL"; // MANUAL or SCHEDULED

    @Column(length = 30, nullable = false)
    @Builder.Default
    private String status = "RUNNING"; // RUNNING, SUCCESS, FAILED, EXPIRED

    @Column(name = "file_path", length = 512)
    private String filePath;

    @Column(name = "storage_path", length = 1000)
    private String storagePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(length = 128)
    private String checksum;

    @Column(name = "started_at")
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "retention_until")
    private LocalDateTime retentionUntil;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
