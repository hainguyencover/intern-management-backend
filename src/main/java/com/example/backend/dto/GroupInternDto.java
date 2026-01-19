package com.example.backend.dto;

public record GroupInternDto(
        Long internId,
        Long userId,
        String fullName,
        String email,
        String university,
        String major
) {}
