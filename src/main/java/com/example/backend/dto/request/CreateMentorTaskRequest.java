package com.example.backend.dto.request;

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
        LocalDateTime dueDate
) {}
