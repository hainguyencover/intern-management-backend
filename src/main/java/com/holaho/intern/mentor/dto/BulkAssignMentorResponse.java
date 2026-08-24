package com.holaho.intern.mentor.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record BulkAssignMentorResponse(
        int totalRequested,
        int successCount,
        int failureCount,
        List<BulkAssignItemResult> results
) {
    @Builder
    public record BulkAssignItemResult(
            Long internId,
            boolean success,
            Long assignmentId,
            String errorCode,
            String errorMessage
    ) {}
}
