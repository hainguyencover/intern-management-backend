package com.holaho.intern.shared.dto.response;

import java.time.LocalDateTime;

public record InternDocumentResponse(
        Long id,
        Long internId,
        String internName,
        String type,
        String originalFileName,
        String fileUrl,
        Long fileSize,
        String contentType,
        String status,
        LocalDateTime uploadedAt,
        Long reviewedById,
        String reviewedByName,
        LocalDateTime reviewedAt,
        String reviewNote,
        String rejectionReason
) {
}
