package com.holaho.intern.task.entity;

import com.holaho.intern.shared.entity.BaseEntity;
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
@Table(name = "task_progress_histories", indexes = {
        @Index(name = "idx_prog_hist_task", columnList = "task_id"),
        @Index(name = "idx_prog_hist_changed_at", columnList = "changed_at")
})
@AttributeOverrides({
        @AttributeOverride(name = "auditCreatedBy", column = @Column(name = "audit_created_by", insertable = false, updatable = false)),
        @AttributeOverride(name = "auditUpdatedBy", column = @Column(name = "audit_updated_by", insertable = false, updatable = false))
})
public class TaskProgressHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)

    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "old_progress")
    private Integer oldProgress;

    @Column(name = "new_progress", nullable = false)
    private Integer newProgress;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    private TaskStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 30)
    private TaskStatus newStatus;

    @Column(length = 1000)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}
