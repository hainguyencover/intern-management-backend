package com.holaho.intern.task.entity;

import com.holaho.intern.entity.ProgramGroup;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.user.entity.User;


import com.holaho.intern.shared.entity.BaseEntity;

import com.holaho.intern.shared.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tasks", indexes = {
                @Index(name = "idx_tasks_status", columnList = "status"),
                @Index(name = "idx_tasks_assignee", columnList = "assignee_intern_id")
})
public class Task extends BaseEntity {

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "group_id", nullable = false)
        private ProgramGroup group;

        @Column(nullable = false, length = 255)
        private String title;

        @Column(length = 4000)
        private String description;

        @Column(name = "due_date")
        private LocalDateTime dueDate;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 30)
        private TaskStatus status = TaskStatus.OPEN;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "created_by")
        private User createdBy;

        // Task được giao cho 1 intern
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "assignee_intern_id")
        private InternProfile assignee;

        @Column(name = "progress_percent")
        private Integer progressPercent = 0;
}
