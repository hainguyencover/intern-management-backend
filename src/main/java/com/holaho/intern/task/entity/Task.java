package com.holaho.intern.task.entity;

import com.holaho.intern.entity.ProgramGroup;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.shared.entity.BaseEntity;
import com.holaho.intern.shared.enums.TaskPriority;
import com.holaho.intern.shared.enums.TaskStatus;
import com.holaho.intern.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_tasks_tenant", columnList = "tenant_id"),
        @Index(name = "idx_tasks_intern", columnList = "intern_id"),
        @Index(name = "idx_tasks_mentor", columnList = "mentor_id"),
        @Index(name = "idx_tasks_status", columnList = "status"),
        @Index(name = "idx_tasks_due_date", columnList = "due_date"),
        @Index(name = "idx_tasks_intern_status", columnList = "intern_id, status"),
        @Index(name = "idx_tasks_mentor_status", columnList = "mentor_id, status")
})
public class Task extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "group_id")
    private ProgramGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intern_id")
    private InternProfile assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id")
    private User mentor;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskStatus status = TaskStatus.OPEN;

    @Column(name = "progress", nullable = false)
    private Integer progressPercent = 0;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private Integer weight = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Version
    private Long version;
}

