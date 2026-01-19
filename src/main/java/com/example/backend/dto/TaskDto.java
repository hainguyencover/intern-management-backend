package com.example.backend.dto;

import com.example.backend.enums.TaskStatus;

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
