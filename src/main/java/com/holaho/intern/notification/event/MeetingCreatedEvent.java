package com.holaho.intern.notification.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MeetingCreatedEvent(
    UUID eventId,
    Long meetingId,
    String meetingTitle,
    Long organizerId,
    String organizerName,
    List<Long> participantIds,
    LocalDateTime startTime,
    Integer durationMinutes,
    String location
) {
}
