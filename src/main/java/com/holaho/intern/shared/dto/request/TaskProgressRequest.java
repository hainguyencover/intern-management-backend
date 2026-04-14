package com.holaho.intern.shared.dto.request;

public record TaskProgressRequest(
        Integer progressPercent,
        String content
) {}

