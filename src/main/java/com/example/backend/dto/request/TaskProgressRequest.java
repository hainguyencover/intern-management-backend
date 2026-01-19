package com.example.backend.dto.request;

public record TaskProgressRequest(
        Integer progressPercent,
        String content
) {}
