package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record CreateMentorTaskRequest(
        @NotNull Long groupId,
        @NotEmpty List<Long> internIds,
        @NotBlank String title,
        String description,
        com.holaho.intern.shared.enums.TaskPriority priority,
        LocalDateTime startDate,
        LocalDateTime dueDate,
        Integer weight
) {}


