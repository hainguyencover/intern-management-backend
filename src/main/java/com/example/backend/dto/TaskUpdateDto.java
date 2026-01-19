package com.example.backend.dto;


import java.time.LocalDateTime;

public record TaskUpdateDto(
        Long id,
        Long taskId,
        Long internId,
        String internName,
        Integer progressPercent,
        String content,
        LocalDateTime createdAt
) {
}
