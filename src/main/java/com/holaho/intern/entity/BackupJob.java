package com.holaho.intern.entity;

import com.holaho.intern.user.entity.User;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "backup_jobs",
        indexes = @Index(name = "idx_backup_status", columnList = "status,started_at")
)
public class BackupJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 32)
    private String type = "MANUAL"; // MANUAL or SCHEDULED

    @Column(length = 16)
    private String status = "RUNNING"; // RUNNING, SUCCESS, FAILED

    @Column(name = "file_path", length = 512)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(length = 128)
    private String checksum;

    @Column(name = "started_at")
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(columnDefinition = "TEXT")
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}

