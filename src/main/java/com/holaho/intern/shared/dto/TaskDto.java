package com.holaho.intern.shared.dto;

import com.holaho.intern.shared.enums.TaskPriority;
import com.holaho.intern.shared.enums.TaskStatus;

import java.time.LocalDateTime;

public record TaskDto(
        Long id,
        Long groupId,
        String title,
        String description,
        TaskPriority priority,
        TaskStatus status,
        Integer progressPercent,
        LocalDateTime startDate,
        LocalDateTime dueDate,
        Integer weight,
        boolean overdue,
        Long assigneeInternId,
        String assigneeName,
        LocalDateTime createdAt
) {}
