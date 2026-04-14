package com.holaho.intern.shared.dto.response;

import java.time.LocalDateTime;

public record InternDocumentResponse(
        Long id,
        Long internId,
        String type,
        String fileUrl,
        String status,
        LocalDateTime uploadedAt,
        Long reviewedById,
        LocalDateTime reviewedAt,
        String reviewNote
) {
}

