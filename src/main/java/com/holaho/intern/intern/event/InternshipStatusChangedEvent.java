package com.holaho.intern.intern.event;

import java.time.Instant;
import java.util.UUID;

public record InternshipStatusChangedEvent(
        String eventId,
        Long internProfileId,
        Long userId,
        Long universityId,
        String studentName,
        String studentCode,
        String previousStatus,
        String newStatus,
        String reason,
        Instant occurredAt
) {
    public static InternshipStatusChangedEvent create(
            Long internProfileId,
            Long userId,
            Long universityId,
            String studentName,
            String studentCode,
            String previousStatus,
            String newStatus,
            String reason
    ) {
        return new InternshipStatusChangedEvent(
                UUID.randomUUID().toString(),
                internProfileId,
                userId,
                universityId,
                studentName,
                studentCode,
                previousStatus,
                newStatus,
                reason,
                Instant.now()
        );
    }
}
