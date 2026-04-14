package com.holaho.intern.shared.dto;

import com.holaho.intern.shared.enums.TaskStatus;

import java.time.LocalDateTime;

public record TaskDto(
        Long id,
        Long groupId,
        String title,
        String description,
        LocalDateTime dueDate,
        TaskStatus status,
        Long assigneeInternId,
        String assigneeName
) {}

